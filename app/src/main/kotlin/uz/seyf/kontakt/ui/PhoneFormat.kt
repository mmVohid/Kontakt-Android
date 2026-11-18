package uz.seyf.kontakt.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Telefon raqami maydonida faqat raqamlar saqlanadi (masalan "998901234567"),
 * ekranda esa "+998 90 123 45 67" ko'rinishida chiqadi. Backspace to'g'ri ishlaydi.
 */
object PhoneFormat {
    const val UZ_PREFIX = "998"
    private val BREAKS = intArrayOf(3, 5, 8, 10)

    fun isBlank(digits: String): Boolean = digits.isEmpty() || digits == UZ_PREFIX

    fun isInvalid(digits: String): Boolean = when {
        isBlank(digits) -> false
        digits.startsWith(UZ_PREFIX) -> digits.length != 12
        else -> digits.length < 7
    }

    fun format(digits: String): String {
        if (digits.isEmpty()) return ""
        val uz = digits.startsWith(UZ_PREFIX)
        val sb = StringBuilder("+")
        digits.forEachIndexed { i, ch ->
            if (uz && i in BREAKS) sb.append(' ')
            sb.append(ch)
        }
        return sb.toString()
    }

    /** Saqlangan matndan maydon holatini tiklaydi. Bo'sh bo'lsa, "998" bilan boshlanadi. */
    fun toFieldDigits(stored: String): String {
        val d = stored.filter { it.isDigit() }
        return if (d.isEmpty()) UZ_PREFIX else d
    }

    fun toStored(digits: String): String = if (isBlank(digits)) "" else format(digits)

    internal fun spaceIndexes(digits: String): List<Int> =
        if (digits.startsWith(UZ_PREFIX)) BREAKS.filter { it < digits.length }.mapIndexed { k, b -> 1 + b + k }
        else emptyList()
}

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val d = text.text
        val spaces = PhoneFormat.spaceIndexes(d)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (d.isEmpty()) return 0
                val o = offset.coerceIn(0, d.length)
                val before = if (d.startsWith(PhoneFormat.UZ_PREFIX)) {
                    intArrayOf(3, 5, 8, 10).count { it < d.length && it <= o }
                } else 0
                return 1 + o + before
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (d.isEmpty()) return 0
                val spacesBefore = spaces.count { it < offset }
                return (offset - 1 - spacesBefore).coerceIn(0, d.length)
            }
        }
        return TransformedText(AnnotatedString(PhoneFormat.format(d)), mapping)
    }
}
