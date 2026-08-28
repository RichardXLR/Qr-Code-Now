package com.richardittou.qrcodenow.domain.generator

import com.google.common.truth.Truth.assertThat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
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
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))))
        assertThat(decoded.text).isEqualTo(content)
    }
}
