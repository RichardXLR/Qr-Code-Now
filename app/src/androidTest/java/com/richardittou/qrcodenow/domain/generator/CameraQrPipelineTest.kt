package com.richardittou.qrcodenow.domain.generator

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.ImageReader
import android.media.ImageWriter
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.richardittou.qrcodenow.data.scanner.MlKitQrScannerEngine
import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the camera engine with real Android YUV buffers, without optical variables. */
@RunWith(AndroidJUnit4::class)
class CameraQrPipelineTest {
    @Test fun generatedQrPassesCameraYuvPipelineRepeatedly() = runBlocking {
        val engine = MlKitQrScannerEngine()
        val payload = "QR Code Now — câmera e imagem"
        val bitmap = ZxingQrEncoder().encode(payload, 512)
        try {
            repeat(3) {
                val reader = ImageReader.newInstance(bitmap.width, bitmap.height, ImageFormat.YUV_420_888, 2)
                val writer = ImageWriter.newInstance(reader.surface, 2)
                try {
                    val input = writer.dequeueInputImage()
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    input.planes.forEachIndexed { index, plane ->
                        val width = if (index == 0) bitmap.width else bitmap.width / 2
                        val height = if (index == 0) bitmap.height else bitmap.height / 2
                        for (y in 0 until height) for (x in 0 until width) {
                            val value = if (index == 0) pixels[y * bitmap.width + x] and 255 else 128
                            plane.buffer.put(y * plane.rowStride + x * plane.pixelStride, value.toByte())
                        }
                    }
                    writer.queueInputImage(input)
                    var media = reader.acquireLatestImage()
                    repeat(100) { if (media == null) { Thread.sleep(10); media = reader.acquireLatestImage() } }
                    val image = requireNotNull(media)
                    val info = Proxy.newProxyInstance(ImageInfo::class.java.classLoader, arrayOf(ImageInfo::class.java)) { _, method, _ ->
                        if (method.name == "getRotationDegrees") 0 else null
                    } as ImageInfo
                    val proxy = Proxy.newProxyInstance(ImageProxy::class.java.classLoader, arrayOf(ImageProxy::class.java)) { _, method, _ ->
                        when (method.name) {
                            "getImage" -> image
                            "getImageInfo" -> info
                            "getWidth" -> image.width
                            "getHeight" -> image.height
                            "getCropRect" -> Rect(0, 0, image.width, image.height)
                            "getPlanes" -> image.planes.map { plane ->
                                Proxy.newProxyInstance(ImageProxy.PlaneProxy::class.java.classLoader, arrayOf(ImageProxy.PlaneProxy::class.java)) { _, call, _ ->
                                    when (call.name) {
                                        "getBuffer" -> plane.buffer
                                        "getPixelStride" -> plane.pixelStride
                                        "getRowStride" -> plane.rowStride
                                        else -> null
                                    }
                                } as ImageProxy.PlaneProxy
                            }.toTypedArray()
                            "close" -> { image.close(); null }
                            else -> null
                        }
                    } as ImageProxy
                    val result = CompletableDeferred<List<String>>()
                    engine.analyze(proxy, { result.complete(it) }, { result.completeExceptionally(it) })
                    assertEquals(listOf(payload), withTimeout(20_000) { result.await() })
                } finally { writer.close(); reader.close() }
            }
        } finally { engine.close() }
    }
}
