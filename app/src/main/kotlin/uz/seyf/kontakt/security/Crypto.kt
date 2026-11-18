package uz.seyf.kontakt.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM (autentifikatsiyali shifrlash) va PBKDF2-HMAC-SHA256 (PIN'dan kalit hosil qilish).
 * Blok formati: [12 bayt IV][shifrlangan matn + 16 bayt GCM tegi].
 * Har bir shifrlashda yangi tasodifiy IV ishlatiladi.
 */
object Crypto {
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val TAG_BYTES = 16
    private val rng = SecureRandom()

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { rng.nextBytes(it) }

    fun encrypt(key: ByteArray, plain: ByteArray, aad: ByteArray): ByteArray {
        val iv = randomBytes(IV_LEN)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad)
        return iv + cipher.doFinal(plain)
    }

    fun decrypt(key: ByteArray, blob: ByteArray, aad: ByteArray): ByteArray {
        require(blob.size > IV_LEN + TAG_BYTES) { "Blok juda qisqa" }
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, blob, 0, IV_LEN))
        cipher.updateAAD(aad)
        return cipher.doFinal(blob, IV_LEN, blob.size - IV_LEN)
    }

    /** PIN'dan 256 bitli kalit. Iteratsiyalar soni sarlavhada saqlanadi. */
    fun deriveKey(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
