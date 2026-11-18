package uz.seyf.kontakt.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore kalitlari. Ular qurilmadan chiqarib bo'lmaydi (non-exportable),
 * imkon bo'lsa apparat xavfsiz muhitida (TEE/StrongBox) saqlanadi.
 *
 *  - DEVICE: PIN bilan o'ralgan kalitni yana bir qavat o'raydi. Fayl nusxalansa ham,
 *    boshqa qurilmada PIN'ni brute-force qilib bo'lmaydi.
 *  - BIO: faqat biometrik autentifikatsiyadan keyin ishlaydi. Yangi barmoq izi
 *    qo'shilsa, kalit avtomatik bekor bo'ladi.
 */
object KeystoreKeys {
    private const val PROVIDER = "AndroidKeyStore"
    private const val DEVICE_ALIAS = "seyf_device_key"
    private const val BIO_ALIAS = "seyf_bio_key"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_LEN = 12

    private fun store(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun baseSpec(alias: String): KeyGenParameterSpec.Builder =
        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

    private fun generate(spec: KeyGenParameterSpec): SecretKey {
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(spec)
        return gen.generateKey()
    }

    fun deviceKey(): SecretKey {
        (store().getKey(DEVICE_ALIAS, null) as? SecretKey)?.let { return it }
        return generate(baseSpec(DEVICE_ALIAS).build())
    }

    fun hasBioKey(): Boolean = store().containsAlias(BIO_ALIAS)

    fun bioKey(): SecretKey? = store().getKey(BIO_ALIAS, null) as? SecretKey

    /** Biometrik kalitni qaytadan yaratadi (eskisi o'chiriladi). */
    fun createBioKey(): SecretKey {
        deleteBioKey()
        val b = baseSpec(BIO_ALIAS)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= 30) {
            b.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            b.setUserAuthenticationValidityDurationSeconds(-1)
        }
        return generate(b.build())
    }

    fun deleteBioKey() {
        val s = store()
        if (s.containsAlias(BIO_ALIAS)) s.deleteEntry(BIO_ALIAS)
    }

    fun deleteAll() {
        val s = store()
        if (s.containsAlias(BIO_ALIAS)) s.deleteEntry(BIO_ALIAS)
        if (s.containsAlias(DEVICE_ALIAS)) s.deleteEntry(DEVICE_ALIAS)
    }

    /** [IV][shifrlangan] */
    fun wrap(key: SecretKey, data: ByteArray): ByteArray {
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, key)
        return c.iv + c.doFinal(data)
    }

    fun unwrap(key: SecretKey, blob: ByteArray): ByteArray {
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob, 0, IV_LEN))
        return c.doFinal(blob, IV_LEN, blob.size - IV_LEN)
    }
}
