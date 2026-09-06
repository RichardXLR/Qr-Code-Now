package com.richardittou.qrcodenow.domain.generator

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.richardittou.qrcodenow.data.scanner.MlKitQrScannerEngine
import com.richardittou.qrcodenow.data.media.QrImageStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import com.richardittou.qrcodenow.data.scanner.MlKitQrBitmapValidator
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneratedQrMlKitTest {
    @Test
    fun generatedCodesRemainReadableByMlKitAfterPreviewScaling() = runBlocking {
        val encoder = ZxingQrEncoder()
        val validator = MlKitQrBitmapValidator()
        val payloads = listOf(
            "https://github.com/RichardXLR",
            "Olá, Richard 👋 日本語",
            "WIFI:T:WPA;S:Rede Casa;P:senha-segura-123;;",
            "conteúdo-áéíóú-1234567890;".repeat(50)
        )

        payloads.forEach { payload ->
            val bitmap = encoder.encode(payload)
            assertTrue("ML Kit must read the generated payload: ${payload.take(40)}", validator.isReadable(bitmap, payload))
        }
    }

    @Test fun savedPngAndCompressedRotatedImagesCanBeReadRepeatedly() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = MlKitQrScannerEngine()
        val bitmap = ZxingQrEncoder().encode("Olá! QR Code Now 👋")
        val saved = QrImageStore().save(context, bitmap).getOrThrow()
        suspend fun read(uri: Uri): List<String> {
            val result = CompletableDeferred<List<String>>()
            engine.scanImage(context, uri, { result.complete(it) }, { result.completeExceptionally(it) })
            return withTimeout(20_000) { result.await() }
        }
        val testFile = File(context.cacheDir, "qr-regression.jpg")
        try {
            repeat(3) { assertEquals(listOf("Olá! QR Code Now 👋"), read(saved)) }
            val rotated = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
            Canvas(rotated).apply {
                drawColor(Color.WHITE)
                rotate(90f, 400f, 400f)
                drawBitmap(Bitmap.createScaledBitmap(bitmap, 600, 600, false), 100f, 100f, null)
            }
            testFile.outputStream().use { rotated.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            assertEquals(listOf("Olá! QR Code Now 👋"), read(Uri.fromFile(testFile)))
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val transparentPixels = pixels.map { if (it == Color.WHITE) Color.TRANSPARENT else it }.toIntArray()
            val transparent = Bitmap.createBitmap(transparentPixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            testFile.outputStream().use { transparent.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertEquals(listOf("Olá! QR Code Now 👋"), read(Uri.fromFile(testFile)))
            val inverted = Bitmap.createBitmap(pixels.map { it xor 0x00ffffff }.toIntArray(), bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            testFile.outputStream().use { inverted.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertEquals(listOf("Olá! QR Code Now 👋"), read(Uri.fromFile(testFile)))
            val blank = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
            testFile.outputStream().use { blank.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(read(Uri.fromFile(testFile)).isEmpty())
            assertEquals(listOf("Olá! QR Code Now 👋"), read(saved))
        } finally {
            context.contentResolver.delete(saved, null, null)
            testFile.delete()
            engine.close()
        }
    }
}
