package com.richardittou.qrcodenow.data.scanner

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader

/** Independent offline decoder, including light modules on a dark background. */
internal object ZxingQrFallback {
    fun decode(bitmap: Bitmap): List<String> {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return decode(RGBLuminanceSource(bitmap.width, bitmap.height, pixels))
    }

    fun cameraSource(image: ImageProxy): LuminanceSource {
        val plane = image.planes.first()
        val buffer = plane.buffer.duplicate()
        val start = buffer.position()
        val crop = image.cropRect
        val bytes = ByteArray(crop.width() * crop.height())
        for (y in 0 until crop.height()) {
            val row = start + (crop.top + y) * plane.rowStride + crop.left * plane.pixelStride
            for (x in 0 until crop.width()) bytes[y * crop.width() + x] = buffer.get(row + x * plane.pixelStride)
        }
        return PlanarYUVLuminanceSource(bytes, crop.width(), crop.height(), 0, 0, crop.width(), crop.height(), false)
    }

    fun decode(source: LuminanceSource): List<String> {
        val hints = mapOf(DecodeHintType.TRY_HARDER to true)
        for (candidate in listOf(source, source.invert())) {
            for (binarizer in listOf(HybridBinarizer(candidate), GlobalHistogramBinarizer(candidate))) {
                try {
                    val values = QRCodeMultiReader().decodeMultiple(BinaryBitmap(binarizer), hints)
                        .mapNotNull { it.text }.filter(String::isNotBlank).distinct()
                    if (values.isNotEmpty()) return values
                } catch (_: ReaderException) {
                    // No complete QR found; continue with the other contrast strategy.
                }
            }
        }
        return emptyList()
    }
}
