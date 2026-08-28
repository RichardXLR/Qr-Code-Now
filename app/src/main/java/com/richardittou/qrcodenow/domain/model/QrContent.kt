package com.richardittou.qrcodenow.domain.model

enum class QrType(val label: String) {
    TEXT("Texto"), URL("URL"), PHONE("Telefone"), SMS("SMS"), EMAIL("E-mail"),
    WIFI("Wi-Fi"), LOCATION("Localização"), CONTACT("Contato"), EVENT("Evento"),
    PIX("PIX"), APP_LINK("Link de aplicativo"), CUSTOM("Personalizado")
}

enum class ScanOrigin { CAMERA, GALLERY, GENERATED }

sealed interface QrContent {
    val raw: String
    val type: QrType
    val title: String

    data class Text(override val raw: String) : QrContent {
        override val type = QrType.TEXT
        override val title = raw.lineSequence().firstOrNull()?.take(72).orEmpty().ifBlank { "Texto" }
    }

    data class Url(override val raw: String, val domain: String, val suspicious: Boolean) : QrContent {
        override val type = QrType.URL
        override val title = domain.ifBlank { raw.take(72) }
    }

    data class Phone(override val raw: String, val number: String) : QrContent {
        override val type = QrType.PHONE
        override val title = number
    }

    data class Sms(override val raw: String, val number: String, val message: String?) : QrContent {
        override val type = QrType.SMS
        override val title = number.ifBlank { "SMS" }
    }

    data class Email(override val raw: String, val address: String, val subject: String?, val body: String?) : QrContent {
        override val type = QrType.EMAIL
        override val title = address
    }

    data class Wifi(
        override val raw: String,
        val ssid: String,
        val password: String?,
        val security: String,
        val hidden: Boolean
    ) : QrContent {
        override val type = QrType.WIFI
        override val title = ssid.ifBlank { "Rede Wi-Fi" }
    }

    data class Location(override val raw: String, val latitude: Double, val longitude: Double) : QrContent {
        override val type = QrType.LOCATION
        override val title = "${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}"
    }

    data class Contact(override val raw: String, val name: String?) : QrContent {
        override val type = QrType.CONTACT
        override val title = name?.takeIf { it.isNotBlank() } ?: "Contato"
    }

    data class Event(override val raw: String, val summary: String?) : QrContent {
        override val type = QrType.EVENT
        override val title = summary?.takeIf { it.isNotBlank() } ?: "Evento"
    }

    data class Pix(override val raw: String, val key: String?) : QrContent {
        override val type = QrType.PIX
        override val title = key?.let { "PIX • ${it.take(48)}" } ?: "Pagamento PIX"
    }

    data class AppLink(override val raw: String, val scheme: String) : QrContent {
        override val type = QrType.APP_LINK
        override val title = "Link $scheme"
    }

    data class Custom(override val raw: String) : QrContent {
        override val type = QrType.CUSTOM
        override val title = "Conteúdo personalizado"
    }
}
