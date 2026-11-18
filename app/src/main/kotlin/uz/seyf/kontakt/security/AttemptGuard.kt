package uz.seyf.kontakt.security

import android.content.Context

/** PIN'ni noto'g'ri kiritish urinishlarini cheklaydi (5 tadan keyin kutish vaqti oshib boradi). */
class AttemptGuard(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("seyf_guard", Context.MODE_PRIVATE)

    val failures: Int get() = prefs.getInt(KEY_FAILS, 0)

    fun attemptsLeft(): Int = (FREE_ATTEMPTS - failures).coerceAtLeast(0)

    fun remainingLockMs(): Long {
        val until = prefs.getLong(KEY_UNTIL, 0L)
        val now = System.currentTimeMillis()
        // Soat orqaga surilsa, ortiqcha uzoq blokdan qochish uchun 2 soatdan oshiq kutishga yo'l qo'ymaymiz.
        return (until - now).coerceIn(0L, 2 * 60 * 60 * 1000L)
    }

    fun onFailure() {
        val n = failures + 1
        val delayMs = when {
            n < FREE_ATTEMPTS -> 0L
            n == 5 -> 30_000L
            n == 6 -> 60_000L
            n == 7 -> 5 * 60_000L
            n == 8 -> 15 * 60_000L
            else -> 60 * 60_000L
        }
        prefs.edit()
            .putInt(KEY_FAILS, n)
            .putLong(KEY_UNTIL, if (delayMs > 0) System.currentTimeMillis() + delayMs else 0L)
            .apply()
    }

    fun onSuccess() {
        prefs.edit().putInt(KEY_FAILS, 0).putLong(KEY_UNTIL, 0L).apply()
    }

    private companion object {
        const val KEY_FAILS = "fails"
        const val KEY_UNTIL = "until"
        const val FREE_ATTEMPTS = 5
    }
}
