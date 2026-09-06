package com.richardittou.qrcodenow.domain.generator

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.nio.charset.StandardCharsets
import javax.inject.Inject

interface QrEncoder {
    fun encode(content: String, size: Int = 1024): Bitmap
}

class ZxingQrEncoder @Inject constructor() : QrEncoder {
    override fun encode(content: String, size: Int): Bitmap {
        if (content.isBlank()) throw QrEncodingException("Digite um conteúdo para gerar o QR Code.")
        if (size !in MIN_BITMAP_SIZE..MAX_BITMAP_SIZE) {
            throw QrEncodingException("O tamanho solicitado para o QR Code não é suportado.")
        }
        if (content.toByteArray(StandardCharsets.UTF_8).size > MAX_SAFE_PAYLOAD_BYTES) {
            throw QrEncodingException("O conteúdo está muito longo. Reduza-o para até $MAX_SAFE_PAYLOAD_BYTES bytes.")
        }
        val hints = mapOf(
            EncodeHintType.MARGIN to QUIET_ZONE_MODULES,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name()
        )
        val writer = MultiFormatWriter()
        val minimumMatrix = runCatching {
            writer.encode(content, BarcodeFormat.QR_CODE, 1, 1, hints)
        }.getOrElse { error ->
            throw QrEncodingException("Este conteúdo não cabe em um QR Code confiável.", error)
        }
        val outputSize = maxOf(size, minimumMatrix.width * MIN_PIXELS_PER_MODULE)
            .coerceAtMost(MAX_BITMAP_SIZE)
        val matrix = runCatching {
            writer.encode(content, BarcodeFormat.QR_CODE, outputSize, outputSize, hints)
        }.getOrElse { error ->
            throw QrEncodingException("Não foi possível codificar este conteúdo.", error)
        }
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            pixels[y * matrix.width + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
        val bitmap = createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        }
        val decoded = MultiFormatReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels))),
            mapOf(DecodeHintType.TRY_HARDER to true)
        ).text
        if (decoded != content) throw QrEncodingException("Falha ao validar o conteúdo codificado.")
        return bitmap
    }

    companion object {
        const val MAX_SAFE_PAYLOAD_BYTES = 2_000
        private const val MIN_BITMAP_SIZE = 256
        private const val MAX_BITMAP_SIZE = 2_048
        private const val MIN_PIXELS_PER_MODULE = 10
        private const val QUIET_ZONE_MODULES = 6
    }
}

class QrEncodingException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
