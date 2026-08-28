package com.richardittou.qrcodenow.domain.parser

import java.net.IDN
import java.net.URI

data class UrlAssessment(val valid: Boolean, val suspicious: Boolean, val domain: String)

object UrlSafety {
    private val ipv4 = Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}$")

    fun assess(value: String): UrlAssessment {
        if (value.length > 2_048 || value.any { it == '\u0000' || it == '\r' || it == '\n' }) {
            return UrlAssessment(false, true, "")
        }
        val uri = runCatching { URI(value) }.getOrNull()
            ?: return UrlAssessment(false, true, "")
        val scheme = uri.scheme?.lowercase()
        val authority = uri.rawAuthority.orEmpty()
        val host = (uri.host ?: authority.substringAfterLast('@').hostWithoutPort())
            .removePrefix("[")
            .removeSuffix("]")
            .trimEnd('.')
        if (scheme !in setOf("http", "https") || host.isNullOrBlank()) {
            return UrlAssessment(false, true, host.orEmpty())
        }
        val numericIp = ipv4.matches(host) || host.contains(':')
        val asciiHost = if (numericIp) host else runCatching { IDN.toASCII(host) }.getOrNull()
            ?: return UrlAssessment(false, true, host)
        val labelsValid = if (numericIp) {
            host.contains(':') || host.split('.').all { it.toIntOrNull() in 0..255 }
        } else {
            asciiHost.length <= 253 && asciiHost.split('.').all { label ->
                label.isNotBlank() && label.length <= 63 && !label.startsWith('-') &&
                    !label.endsWith('-') && label.all { it.isLetterOrDigit() || it == '-' }
            }
        }
        val embeddedCredentials = !uri.rawUserInfo.isNullOrBlank()
        val unicodeHost = host.any { it.code > 127 }
        val invalidPort = uri.port !in -1..65_535 || uri.port == 0
        val suspicious = !labelsValid || invalidPort || embeddedCredentials || unicodeHost || numericIp ||
            asciiHost.split('.').any { it.startsWith("xn--", ignoreCase = true) } || value.count { it == '%' } > 12
        return UrlAssessment(labelsValid && !invalidPort, suspicious, host)
    }

    private fun String.hostWithoutPort(): String = when {
        startsWith('[') -> substringBefore(']') + "]"
        count { it == ':' } == 1 -> substringBeforeLast(':')
        else -> this
    }
}
