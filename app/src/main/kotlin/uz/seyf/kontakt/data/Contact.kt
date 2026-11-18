package uz.seyf.kontakt.data

import org.json.JSONObject

data class Contact(
    val id: String,
    val firstName: String = "",
    val lastName: String = "",
    val patronymic: String = "",
    val phone: String = "",
    val passportSeries: String = "",
    val pinfl: String = "",
    val address: String = "",
    val carPlate: String = "",
    val note: String = "",
    val hasPhoto: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    /** Ro'yxatda ko'rinadigan nom: "Ism Familya" */
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            .ifBlank { phone.ifBlank { "Nomsiz kontakt" } }

    val initials: String
        get() {
            val a = firstName.trim().firstOrNull { it.isLetter() }
            val b = lastName.trim().firstOrNull { it.isLetter() }
            return listOfNotNull(a, b).joinToString("") { it.uppercaseChar().toString() }
        }

    /** Ro'yxatdagi harf bo'limi */
    fun letter(): String =
        displayName.firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "#"

    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        val haystack = listOf(firstName, lastName, patronymic, phone, passportSeries, pinfl, address, carPlate, note)
            .joinToString(" ").lowercase()
        if (haystack.contains(q)) return true
        val qDigits = q.filter { it.isDigit() }
        if (qDigits.length >= 3 && phone.filter { it.isDigit() }.contains(qDigits)) return true
        val qPlate = q.replace(" ", "")
        return qPlate.length >= 2 && carPlate.replace(" ", "").lowercase().contains(qPlate)
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("fn", firstName).put("ln", lastName).put("pt", patronymic)
        .put("ph", phone).put("ps", passportSeries).put("pf", pinfl).put("ad", address)
        .put("cp", carPlate).put("nt", note).put("hp", hasPhoto)
        .put("c", createdAt).put("u", updatedAt)

    companion object {
        fun fromJson(o: JSONObject): Contact = Contact(
            id = o.getString("id"),
            firstName = o.optString("fn"),
            lastName = o.optString("ln"),
            patronymic = o.optString("pt"),
            phone = o.optString("ph"),
            passportSeries = o.optString("ps"),
            pinfl = o.optString("pf"),
            address = o.optString("ad"),
            carPlate = o.optString("cp"),
            note = o.optString("nt"),
            hasPhoto = o.optBoolean("hp", false),
            createdAt = o.optLong("c", 0L),
            updatedAt = o.optLong("u", 0L),
        )

        val ORDER: Comparator<Contact> =
            compareBy<Contact>({ it.firstName.lowercase() }, { it.lastName.lowercase() }, { it.phone })
    }
}
