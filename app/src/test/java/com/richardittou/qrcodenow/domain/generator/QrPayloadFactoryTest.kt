package com.richardittou.qrcodenow.domain.generator

import com.google.common.truth.Truth.assertThat
import com.richardittou.qrcodenow.domain.model.QrType
import org.junit.Test

class QrPayloadFactoryTest {
    @Test fun `adds HTTPS to URL without scheme`() {
        assertThat(QrPayloadFactory.build(GeneratorInput(QrType.URL, "example.com"))).isEqualTo("https://example.com")
    }

    @Test fun `escapes Wi-Fi reserved characters`() {
        val payload = QrPayloadFactory.build(GeneratorInput(QrType.WIFI, "Rede;Casa", "abc:1234", "WPA2"))
        assertThat(payload).isEqualTo("WIFI:T:WPA2;S:Rede\\;Casa;P:abc\\:1234;;")
    }

    @Test fun `rejects invalid coordinates`() {
        assertThat(QrPayloadFactory.build(GeneratorInput(QrType.LOCATION, "100", "10"))).isNull()
    }

    @Test fun `creates a valid vCard`() {
        val payload = QrPayloadFactory.build(GeneratorInput(QrType.CONTACT, "Richard Ittou", "+5511999999999", "dev@example.com"))
        assertThat(payload).contains("BEGIN:VCARD")
        assertThat(payload).contains("FN:Richard Ittou")
        assertThat(payload).contains("END:VCARD")
    }

    @Test fun `rejects invalid structured fields`() {
        assertThat(QrPayloadFactory.build(GeneratorInput(QrType.URL, "não é url"))).isNull()
        assertThat(QrPayloadFactory.build(GeneratorInput(QrType.EMAIL, "email-invalido"))).isNull()
        assertThat(QrPayloadFactory.build(GeneratorInput(QrType.PHONE, "abc"))).isNull()
        assertThat(QrPayloadFactory.build(GeneratorInput(QrType.EVENT, "Evento", "20261340"))).isNull()
        assertThat(QrPayloadFactory.build(GeneratorInput(QrType.WIFI, "Rede", "curta", "WPA2"))).isNull()
    }

    @Test fun `rejects content made only of invisible characters`() {
        assertThat(QrPayloadFactory.build(GeneratorInput(QrType.CUSTOM, "\u200B\u200D"))).isNull()
    }
}
