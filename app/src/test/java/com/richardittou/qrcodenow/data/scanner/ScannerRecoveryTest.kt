package com.richardittou.qrcodenow.data.scanner

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.richardittou.qrcodenow.domain.generator.ZxingQrEncoder
import java.lang.reflect.Proxy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScannerRecoveryTest {
    @Test fun `bad camera frame does not block the next frame`() {
        val scanner = Proxy.newProxyInstance(BarcodeScanner::class.java.classLoader, arrayOf(BarcodeScanner::class.java)) { _, _, _ -> null } as BarcodeScanner
        val engine = MlKitQrScannerEngine(scanner)
        var reads = 0
        var closes = 0
        val image = Proxy.newProxyInstance(ImageProxy::class.java.classLoader, arrayOf(ImageProxy::class.java)) { _, method, _ ->
            when (method.name) {
                "getImage" -> { reads++; if (reads == 1) throw IllegalStateException("Frame no longer available") else null }
                "close" -> { closes++; null }
                else -> null
            }
        } as ImageProxy
        try {
            engine.analyze(image, {}, {})
            engine.analyze(image, {}, {})
            assertThat(reads).isEqualTo(2)
            assertThat(closes).isEqualTo(2)
        } finally { engine.close() }
    }

    @Test fun `fallback reads inverted QR without reporting blank images`() {
        val payload = "https://example.com/qr?texto=olá"
        val bitmap = ZxingQrEncoder().encode(payload)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (index in pixels.indices) pixels[index] = pixels[index] xor 0x00ffffff
        val inverted = Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        assertThat(ZxingQrFallback.decode(inverted)).containsExactly(payload)
        val blank = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        assertThat(ZxingQrFallback.decode(blank)).isEmpty()
    }
}
