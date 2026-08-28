package com.richardittou.qrcodenow.domain.parser

import com.google.common.truth.Truth.assertThat
import com.richardittou.qrcodenow.domain.model.QrContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class QrContentParserTest {
    private val parser = DefaultQrContentParser()

    @Test fun `parses safe https URL and exposes domain`() {
        val parsed = parser.parse("https://github.com/RichardXLR") as QrContent.Url
        assertThat(parsed.domain).isEqualTo("github.com")
        assertThat(parsed.suspicious).isFalse()
    }

    @Test fun `flags URLs with embedded credentials`() {
        val parsed = parser.parse("https://conta@exemplo.com/login") as QrContent.Url
        assertThat(parsed.suspicious).isTrue()
    }

    @Test fun `parses escaped Wi-Fi fields`() {
        val parsed = parser.parse("WIFI:T:WPA;S:Minha\\;Rede;P:segredo\\:123;;") as QrContent.Wifi
        assertThat(parsed.ssid).isEqualTo("Minha;Rede")
        assertThat(parsed.password).isEqualTo("segredo:123")
        assertThat(parsed.security).isEqualTo("WPA")
    }

    @Test fun `parses lower case Wi-Fi prefix and parameterized vCard name`() {
        val wifi = parser.parse("wifi:T:WPA2;S:Casa;P:12345678;;") as QrContent.Wifi
        assertThat(wifi.ssid).isEqualTo("Casa")
        val contact = parser.parse("BEGIN:VCARD\nVERSION:3.0\nFN;CHARSET=UTF-8:Richard Ittou\nEND:VCARD") as QrContent.Contact
        assertThat(contact.name).isEqualTo("Richard Ittou")
    }

    @Test fun `parses phone email SMS and geo`() {
        assertThat((parser.parse("tel:+5511999999999") as QrContent.Phone).number).isEqualTo("+5511999999999")
        assertThat((parser.parse("mailto:dev@example.com?subject=Oi") as QrContent.Email).address).isEqualTo("dev@example.com")
        assertThat((parser.parse("SMSTO:+5511988887777:Olá") as QrContent.Sms).message).isEqualTo("Olá")
        val geo = parser.parse("geo:-23.55052,-46.63331") as QrContent.Location
        assertThat(geo.latitude).isWithin(0.00001).of(-23.55052)
    }

    @Test fun `recognizes PIX EMV payload`() {
        val parsed = parser.parse("00020126580014BR.GOV.BCB.PIX0136chave@example.com5204000053039865802BR")
        assertThat(parsed).isInstanceOf(QrContent.Pix::class.java)
    }

    @Test fun `keeps unknown content as text`() {
        val parsed = parser.parse("mensagem local")
        assertThat(parsed).isEqualTo(QrContent.Text("mensagem local"))
    }
}
