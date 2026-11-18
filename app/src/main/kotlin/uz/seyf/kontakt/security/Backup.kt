package uz.seyf.kontakt.security

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import uz.seyf.kontakt.data.Contact
import java.nio.ByteBuffer
import java.security.GeneralSecurityException

/**
 * Zaxira nusxa fayli. Foydalanuvchi tanlagan PAROL bilan shifrlanadi
 * (PIN emas, chunki boshqa telefonda boshqa PIN bo'lishi mumkin).
 *
 * Format: "SEYFBAK1" | iteratsiyalar (4 bayt) | tuz (16 bayt) | AES-256-GCM blok
 * Ichida: kontaktlar JSON'i va rasmlar (base64).
 */
object Backup {
    private val MAGIC = "SEYFBAK1".toByteArray(Charsets.UTF_8)
    private const val ITERATIONS = 310_000
    private const val SALT_LEN = 16
    private val ID_REGEX = Regex("^[A-Za-z0-9-]{8,64}$")

    class BadBackupException(message: String) : Exception(message)

    data class Parsed(val contacts: List<Contact>, val photos: Map<String, ByteArray>)

    fun build(contacts: List<Contact>, photo: (String) -> ByteArray?, password: CharArray): ByteArray {
        val arr = JSONArray()
        contacts.forEach { arr.put(it.toJson()) }
        val photos = JSONObject()
        contacts.filter { it.hasPhoto }.forEach { c ->
            photo(c.id)?.let { photos.put(c.id, Base64.encodeToString(it, Base64.NO_WRAP)) }
        }
        val root = JSONObject().put("v", 1).put("contacts", arr).put("photos", photos)

        val salt = Crypto.randomBytes(SALT_LEN)
        val key = Crypto.deriveKey(password, salt, ITERATIONS)
        try {
            val blob = Crypto.encrypt(key, root.toString().toByteArray(Charsets.UTF_8), MAGIC)
            val iter = ByteBuffer.allocate(4).putInt(ITERATIONS).array()
            return MAGIC + iter + salt + blob
        } finally {
            key.fill(0)
        }
    }

    fun parse(data: ByteArray, password: CharArray): Parsed {
        val headerLen = MAGIC.size + 4 + SALT_LEN
        if (data.size < headerLen + 40 || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BadBackupException("Bu Seyf Kontakt zaxira fayli emas")
        }
        val iterations = ByteBuffer.wrap(data, MAGIC.size, 4).int
        if (iterations !in 50_000..2_000_000) throw BadBackupException("Zaxira fayli shikastlangan")
        val salt = data.copyOfRange(MAGIC.size + 4, headerLen)
        val blob = data.copyOfRange(headerLen, data.size)

        val key = Crypto.deriveKey(password, salt, iterations)
        val plain = try {
            Crypto.decrypt(key, blob, MAGIC)
        } catch (e: GeneralSecurityException) {
            throw BadBackupException("Parol noto'g'ri yoki fayl shikastlangan")
        } finally {
            key.fill(0)
        }

        try {
            val root = JSONObject(String(plain, Charsets.UTF_8))
            val arr = root.getJSONArray("contacts")
            val contacts = ArrayList<Contact>(arr.length())
            for (i in 0 until arr.length()) {
                val c = Contact.fromJson(arr.getJSONObject(i))
                if (c.id.matches(ID_REGEX)) contacts.add(c)
            }
            val photosJson = root.optJSONObject("photos") ?: JSONObject()
            val photos = HashMap<String, ByteArray>()
            for (c in contacts) {
                val b64 = photosJson.optString(c.id, "")
                if (b64.isNotEmpty()) photos[c.id] = Base64.decode(b64, Base64.NO_WRAP)
            }
            return Parsed(contacts, photos)
        } catch (e: Exception) {
            throw BadBackupException("Zaxira fayli shikastlangan")
        } finally {
            plain.fill(0)
        }
    }
}
