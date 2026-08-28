package com.richardittou.qrcodenow.domain.generator

import com.richardittou.qrcodenow.domain.model.QrType
import com.richardittou.qrcodenow.domain.parser.UrlSafety
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class GeneratorInput(
    val type: QrType,
    val primary: String,
    val secondary: String = "",
    val tertiary: String = "",
    val extra: String = ""
)

object QrPayloadFactory {
    fun build(input: GeneratorInput): String? {
        val primary = input.primary.trim()
        if (primary.isBlank()) return null
        return when (input.type) {
            QrType.TEXT, QrType.CUSTOM -> primary
            QrType.PIX -> primary.takeIf { it.startsWith("000201") && it.contains("BR.GOV.BCB.PIX", true) }
            QrType.APP_LINK -> primary.takeIf { value ->
                runCatching { java.net.URI(value).scheme?.isNotBlank() == true }.getOrDefault(false)
            }
            QrType.URL -> normalizeUrl(primary).takeIf { UrlSafety.assess(it).valid }
            QrType.PHONE -> primary.filterPhone().takeIf { value -> value.count { it.isDigit() } >= 3 }?.let { "tel:$it" }
            QrType.SMS -> primary.filterPhone().takeIf { value -> value.count { it.isDigit() } >= 3 }
                ?.let { "SMSTO:$it:${input.secondary}" }
            QrType.EMAIL -> primary.takeIf(::isEmail)?.let {
                "mailto:${UriCodec.encode(it)}?subject=${UriCodec.encode(input.secondary)}&body=${UriCodec.encode(input.tertiary)}"
            }
            QrType.WIFI -> {
                val security = input.tertiary.trim().ifBlank { "WPA2" }.uppercase()
                val validPassword = when (security) {
                    "NOPASS" -> true
                    "WEP" -> input.secondary.length in setOf(5, 10, 13, 26)
                    else -> input.secondary.length in 8..63
                }
                if (security !in WIFI_SECURITY || !validPassword) return null
                "WIFI:T:${security.wifiEscape()};S:${primary.wifiEscape()};P:${input.secondary.wifiEscape()};;"
            }
            QrType.LOCATION -> {
                val lat = primary.toDoubleOrNull() ?: return null
                val lon = input.secondary.trim().toDoubleOrNull() ?: return null
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
                "geo:$lat,$lon"
            }
            QrType.CONTACT -> buildString {
                if (input.secondary.isNotBlank() && input.secondary.filterPhone().count { it.isDigit() } < 3) return null
                if (input.tertiary.isNotBlank() && !isEmail(input.tertiary.trim())) return null
                appendLine("BEGIN:VCARD")
                appendLine("VERSION:3.0")
                appendLine("FN:${primary.vCardEscape()}")
                if (input.secondary.isNotBlank()) appendLine("TEL:${input.secondary.filterPhone()}")
                if (input.tertiary.isNotBlank()) appendLine("EMAIL:${input.tertiary.vCardEscape()}")
                append("END:VCARD")
            }
            QrType.EVENT -> buildString {
                val dateTime = input.secondary.filter { it.isLetterOrDigit() }
                if (dateTime.isNotBlank() && !isValidEventDate(dateTime)) return null
                appendLine("BEGIN:VEVENT")
                appendLine("SUMMARY:${primary.vCardEscape()}")
                if (dateTime.isNotBlank()) appendLine("DTSTART:$dateTime")
                if (input.tertiary.isNotBlank()) appendLine("LOCATION:${input.tertiary.vCardEscape()}")
                append("END:VEVENT")
            }
        }
    }

    private fun normalizeUrl(value: String): String = if (value.startsWith("http://", true) || value.startsWith("https://", true)) value else "https://$value"
    private fun String.filterPhone() = filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
    private fun String.wifiEscape() = replace("\\", "\\\\").replace(";", "\\;").replace(":", "\\:").replace(",", "\\,")
    private fun String.vCardEscape() = replace("\\", "\\\\").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")

    private fun isValidEventDate(value: String): Boolean {
        if (!EVENT_DATE.matches(value)) return false
        return runCatching {
            LocalDate.parse(value.take(8), DateTimeFormatter.BASIC_ISO_DATE)
            if ('T' in value) {
                val time = value.substringAfter('T').removeSuffix("Z")
                require(time.substring(0, 2).toInt() in 0..23)
                require(time.substring(2, 4).toInt() in 0..59)
                require(time.substring(4, 6).toInt() in 0..59)
            }
        }.isSuccess
    }

    private val EMAIL = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    private fun isEmail(value: String) = EMAIL.matches(value)
    private val EVENT_DATE = Regex("^\\d{8}(T\\d{6}Z?)?$")
    private val WIFI_SECURITY = setOf("WPA", "WPA2", "WPA3", "WEP", "NOPASS")
}

private object UriCodec {
    fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
