package com.richardittou.qrcodenow.domain.generator

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject

interface QrEncoder {
    fun encode(content: String, size: Int = 1024): Bitmap
}

class ZxingQrEncoder @Inject constructor() : QrEncoder {
    override fun encode(content: String, size: Int): Bitmap {
        require(content.isNotBlank())
        require(size >= 256)
        val matrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
        )
        val pixels = IntArray(size * size)
        for (y in 0 until size) for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
        val decoded = MultiFormatReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))
        ).text
        check(decoded == content) { "Falha ao validar o QR Code gerado" }
        return bitmap
    }
}
