package com.richardittou.qrcodenow.domain.parser

import android.net.Uri
import androidx.core.net.toUri
import com.richardittou.qrcodenow.domain.model.QrContent
import javax.inject.Inject

interface QrContentParser {
    fun parse(rawValue: String): QrContent
}

class DefaultQrContentParser @Inject constructor() : QrContentParser {
    override fun parse(rawValue: String): QrContent {
        val raw = rawValue.trim().take(MAX_CONTENT_LENGTH)
        if (raw.isBlank()) return QrContent.Custom(raw)

        if (raw.startsWith("WIFI:", true)) return parseWifi(raw)
        if (raw.startsWith("BEGIN:VCARD", true) || raw.startsWith("MECARD:", true)) {
            return QrContent.Contact(raw, field(raw, "FN") ?: field(raw, "N"))
        }
        if (raw.startsWith("BEGIN:VEVENT", true)) {
            return QrContent.Event(raw, field(raw, "SUMMARY"))
        }
        if (isPix(raw)) return QrContent.Pix(raw, pixKey(raw))

        val uri = runCatching { raw.toUri() }.getOrNull()
        when (uri?.scheme?.lowercase()) {
            "http", "https" -> {
                val assessment = UrlSafety.assess(raw)
                return if (assessment.valid) QrContent.Url(raw, assessment.domain, assessment.suspicious)
                else QrContent.Custom(raw)
            }
            "tel" -> return QrContent.Phone(raw, uri.schemeSpecificPart.substringBefore('?'))
            "sms", "smsto" -> return parseSms(raw, uri)
            "mailto" -> return QrContent.Email(
                raw = raw,
                address = uri.schemeSpecificPart.substringBefore('?'),
                subject = queryParam(raw, "subject"),
                body = queryParam(raw, "body")
            )
            "geo" -> parseGeo(uri)?.let { return it.copy(raw = raw) }
            null -> Unit
            else -> return QrContent.AppLink(raw, uri.scheme.orEmpty())
        }

        if (EMAIL.matches(raw)) return QrContent.Email(raw, raw, null, null)
        if (PHONE.matches(raw)) return QrContent.Phone(raw, raw)
        return QrContent.Text(raw)
    }

    private fun parseSms(raw: String, uri: Uri): QrContent.Sms {
        val specific = uri.schemeSpecificPart
        val number = specific.substringBefore('?').substringBefore(':')
        val inlineMessage = specific.substringAfter(':', "").substringBefore('?').ifBlank { null }
        return QrContent.Sms(raw, number, queryParam(raw, "body") ?: inlineMessage)
    }

    private fun parseGeo(uri: Uri): QrContent.Location? {
        val coordinates = uri.schemeSpecificPart.substringBefore('?').split(',')
        if (coordinates.size < 2) return null
        val lat = coordinates[0].toDoubleOrNull() ?: return null
        val lon = coordinates[1].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return QrContent.Location("", lat, lon)
    }

    private fun parseWifi(raw: String): QrContent.Wifi {
        val fields = parseEscapedFields(raw.substring(5).removeSuffix(";;"))
        return QrContent.Wifi(
            raw = raw,
            ssid = fields["S"].orEmpty(),
            password = fields["P"],
            security = fields["T"].orEmpty().ifBlank { "nopass" },
            hidden = fields["H"].equals("true", true)
        )
    }

    private fun parseEscapedFields(input: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val token = StringBuilder()
        val parts = mutableListOf<String>()
        var escaped = false
        input.forEach { char ->
            when {
                escaped -> { token.append(char); escaped = false }
                char == '\\' -> escaped = true
                char == ';' -> { parts += token.toString(); token.clear() }
                else -> token.append(char)
            }
        }
        if (token.isNotEmpty()) parts += token.toString()
        parts.forEach { part ->
            val separator = part.indexOf(':')
            if (separator > 0) result[part.substring(0, separator).uppercase()] = part.substring(separator + 1)
        }
        return result
    }

    private fun isPix(value: String): Boolean = value.startsWith("000201") &&
        (value.contains("BR.GOV.BCB.PIX", true) || value.contains("br.gov.bcb.pix", true))

    private fun pixKey(value: String): String? {
        val marker = value.indexOf("BR.GOV.BCB.PIX", ignoreCase = true)
        if (marker < 0) return null
        val tail = value.substring(marker + 14)
        val keyMarker = tail.indexOf("01")
        if (keyMarker < 0 || keyMarker + 4 > tail.length) return null
        val length = tail.substring(keyMarker + 2, keyMarker + 4).toIntOrNull() ?: return null
        return tail.drop(keyMarker + 4).take(length).takeIf { it.length == length }
    }

    private fun field(raw: String, name: String): String? = raw.lineSequence()
        .firstOrNull { line -> line.substringBefore(':').substringBefore(';').equals(name, true) }
        ?.substringAfter(':')
        ?.trim()

    private fun queryParam(raw: String, name: String): String? = raw.substringAfter('?', "")
        .split('&')
        .firstOrNull { it.substringBefore('=').equals(name, true) }
        ?.substringAfter('=', "")
        ?.let(Uri::decode)

    companion object {
        private const val MAX_CONTENT_LENGTH = 64 * 1024
        private val EMAIL = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
        private val PHONE = Regex("^\\+?[0-9][0-9 .()/-]{5,24}$")
    }
}
