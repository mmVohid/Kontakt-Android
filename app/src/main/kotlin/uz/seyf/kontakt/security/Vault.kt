package uz.seyf.kontakt.security

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import uz.seyf.kontakt.data.Contact
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Shifrlangan ombor.
 *
 * Kalit ierarxiyasi:
 *   DEK (tasodifiy 256 bit)  — barcha kontakt va rasmlarni AES-256-GCM bilan shifrlaydi
 *   DEK  --PIN(PBKDF2)-->  --Keystore(DEVICE kalit)-->  keys.json
 *   DEK  --Keystore(BIO kalit, biometrika talab qiladi)-->  bio.json   (ixtiyoriy)
 *
 * DEK faqat ilova ochiq paytida xotirada turadi, bloklanganda nollar bilan o'chiriladi.
 */
class Vault(ctx: Context) {
    private val dir = File(ctx.filesDir, "vault")
    private val photosDir = File(dir, "photos")
    private val headerFile = File(dir, "keys.json")
    private val dataFile = File(dir, "contacts.bin")
    private val bioFile = File(dir, "bio.json")

    @Volatile
    private var dek: ByteArray? = null

    init {
        photosDir.mkdirs()
    }

    val isSetUp: Boolean get() = headerFile.exists()
    val isUnlocked: Boolean get() = dek != null

    // ---------- PIN ----------

    fun setupPin(pin: CharArray) {
        val newDek = Crypto.randomBytes(32)
        writeHeader(pin, newDek)
        dek?.fill(0)
        dek = newDek
    }

    fun unlockWithPin(pin: CharArray): Boolean {
        val recovered = recoverDek(pin) ?: return false
        dek?.fill(0)
        dek = recovered
        return true
    }

    /** PINni tekshiradi, lekin ombor holatini o'zgartirmaydi. */
    fun verifyPin(pin: CharArray): Boolean {
        val d = recoverDek(pin) ?: return false
        d.fill(0)
        return true
    }

    fun changePin(oldPin: CharArray, newPin: CharArray): Boolean {
        val d = recoverDek(oldPin) ?: return false
        writeHeader(newPin, d)
        dek?.fill(0)
        dek = d
        return true
    }

    fun lock() {
        dek?.fill(0)
        dek = null
    }

    private fun writeHeader(pin: CharArray, key: ByteArray) {
        val salt = Crypto.randomBytes(16)
        val kek = Crypto.deriveKey(pin, salt, PBKDF2_ITERATIONS)
        try {
            val inner = Crypto.encrypt(kek, key, AAD_KEK)
            val outer = KeystoreKeys.wrap(KeystoreKeys.deviceKey(), inner)
            val json = JSONObject()
                .put("v", 1)
                .put("it", PBKDF2_ITERATIONS)
                .put("salt", b64(salt))
                .put("blob", b64(outer))
            writeAtomic(headerFile, json.toString().toByteArray(Charsets.UTF_8))
        } finally {
            kek.fill(0)
        }
    }

    private fun recoverDek(pin: CharArray): ByteArray? {
        if (!headerFile.exists()) return null
        return try {
            val h = JSONObject(headerFile.readText(Charsets.UTF_8))
            val salt = unb64(h.getString("salt"))
            val iterations = h.getInt("it")
            val outer = unb64(h.getString("blob"))
            val inner = KeystoreKeys.unwrap(KeystoreKeys.deviceKey(), outer)
            val kek = Crypto.deriveKey(pin, salt, iterations)
            try {
                Crypto.decrypt(kek, inner, AAD_KEK)
            } finally {
                kek.fill(0)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Biometrika ----------

    fun biometricEnabled(): Boolean = bioFile.exists() && KeystoreKeys.hasBioKey()

    /** Yoqish: ENCRYPT rejimidagi Cipher. BiometricPrompt muvaffaqiyatidan keyin [bioEnableFinish] chaqiriladi. */
    fun bioEnableCipher(): Cipher {
        val key = KeystoreKeys.createBioKey()
        return Cipher.getInstance(BIO_TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    fun bioEnableFinish(cipher: Cipher) {
        val k = requireKey()
        val ct = cipher.doFinal(k)
        val json = JSONObject().put("iv", b64(cipher.iv)).put("ct", b64(ct))
        writeAtomic(bioFile, json.toString().toByteArray(Charsets.UTF_8))
    }

    /** Ochish: DECRYPT rejimidagi Cipher. Biometrika o'zgargan bo'lsa null qaytadi va funksiya o'chiriladi. */
    fun bioUnlockCipher(): Cipher? {
        if (!biometricEnabled()) return null
        return try {
            val j = JSONObject(bioFile.readText(Charsets.UTF_8))
            val key = KeystoreKeys.bioKey() ?: return null
            Cipher.getInstance(BIO_TRANSFORM).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, unb64(j.getString("iv"))))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            disableBiometric()
            null
        } catch (e: Exception) {
            null
        }
    }

    fun bioUnlockFinish(cipher: Cipher): Boolean = try {
        val j = JSONObject(bioFile.readText(Charsets.UTF_8))
        val recovered = cipher.doFinal(unb64(j.getString("ct")))
        dek?.fill(0)
        dek = recovered
        true
    } catch (e: Exception) {
        false
    }

    fun disableBiometric() {
        bioFile.delete()
        try {
            KeystoreKeys.deleteBioKey()
        } catch (_: Exception) {
        }
    }

    // ---------- Kontaktlar ----------

    fun loadContacts(): List<Contact> {
        val k = requireKey()
        if (!dataFile.exists()) return emptyList()
        val json = String(Crypto.decrypt(k, dataFile.readBytes(), AAD_DATA), Charsets.UTF_8)
        val arr = JSONArray(json)
        return List(arr.length()) { Contact.fromJson(arr.getJSONObject(it)) }
    }

    fun saveContacts(list: List<Contact>) {
        val k = requireKey()
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val blob = Crypto.encrypt(k, arr.toString().toByteArray(Charsets.UTF_8), AAD_DATA)
        writeAtomic(dataFile, blob)
    }

    // ---------- Rasmlar ----------

    fun savePhoto(id: String, jpeg: ByteArray) {
        val k = requireKey()
        writeAtomic(photoFile(id), Crypto.encrypt(k, jpeg, photoAad(id)))
    }

    fun loadPhoto(id: String): ByteArray? {
        val k = requireKey()
        val f = photoFile(id)
        if (!f.exists()) return null
        return Crypto.decrypt(k, f.readBytes(), photoAad(id))
    }

    fun deletePhoto(id: String) {
        photoFile(id).delete()
    }

    // ---------- To'liq tozalash ----------

    /** Hamma narsani qaytarib bo'lmaydigan tarzda o'chiradi (PIN unutilganda). */
    fun wipe() {
        lock()
        dir.deleteRecursively()
        photosDir.mkdirs()
        try {
            KeystoreKeys.deleteAll()
        } catch (_: Exception) {
        }
    }

    // ---------- Yordamchilar ----------

    private fun requireKey(): ByteArray = dek ?: throw IllegalStateException("Ombor bloklangan")

    private fun photoFile(id: String): File {
        require(id.matches(ID_REGEX)) { "Noto'g'ri id" }
        return File(photosDir, "$id.bin")
    }

    private fun photoAad(id: String): ByteArray = "seyf/photo/$id".toByteArray(Charsets.UTF_8)

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    private companion object {
        const val PBKDF2_ITERATIONS = 200_000
        const val BIO_TRANSFORM = "AES/GCM/NoPadding"
        val AAD_KEK = "seyf/kek/v1".toByteArray(Charsets.UTF_8)
        val AAD_DATA = "seyf/contacts/v1".toByteArray(Charsets.UTF_8)
        val ID_REGEX = Regex("^[A-Za-z0-9-]{8,64}$")
    }
}
