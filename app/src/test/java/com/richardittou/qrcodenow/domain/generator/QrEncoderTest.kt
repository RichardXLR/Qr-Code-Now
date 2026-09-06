package com.richardittou.qrcodenow.domain.generator

import com.google.common.truth.Truth.assertThat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class QrEncoderTest {
    @Test fun `generated bitmap can be decoded again`() {
        val content = "https://github.com/RichardXLR"
        val bitmap = ZxingQrEncoder().encode(content, 512)
        assertThat(decode(bitmap)).isEqualTo(content)
    }

    @Test fun `unicode content is encoded as UTF-8`() {
        val content = "Olá, Richard 👋 日本語"
        val bitmap = ZxingQrEncoder().encode(content)

        assertThat(decode(bitmap)).isEqualTo(content)
    }

    @Test fun `dense content receives a larger bitmap`() {
        val content = "conteúdo-áéíóú-1234567890;".repeat(50)
        val bitmap = ZxingQrEncoder().encode(content)

        assertThat(bitmap.width).isGreaterThan(1024)
        assertThat(decode(bitmap)).isEqualTo(content)
    }

    @Test fun `content above the reliable byte limit is rejected`() {
        val error = assertThrows(QrEncodingException::class.java) {
            ZxingQrEncoder().encode("x".repeat(ZxingQrEncoder.MAX_SAFE_PAYLOAD_BYTES + 1))
        }

        assertThat(error).hasMessageThat().contains("muito longo")
    }

    private fun decode(bitmap: android.graphics.Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))), mapOf(DecodeHintType.TRY_HARDER to true))
        return decoded.text
    }
}
