package com.richardittou.qrcodenow.data.scanner

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.richardittou.qrcodenow.domain.scanner.QrScannerEngine
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlKitQrScannerEngine internal constructor(private val scanner: BarcodeScanner) : QrScannerEngine {
    @Inject constructor() : this(BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    ))

    private val busy = AtomicBoolean(false)
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var lastFallbackAt = 0L

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy, onResult: (List<String>) -> Unit, onError: (Throwable) -> Unit) {
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val released = AtomicBoolean(false)
        fun releaseFrame() {
            if (released.compareAndSet(false, true)) {
                try { imageProxy.close() } finally { busy.set(false) }
            }
        }
        fun fallbackOrFinish(values: List<String>, failure: Exception?) {
            val now = System.nanoTime() / 1_000_000L
            if (values.isNotEmpty() || now - lastFallbackAt < 700) {
                releaseFrame()
                if (failure != null) onError(failure) else onResult(values)
                return
            }
            lastFallbackAt = now
            try {
                worker.execute {
                    val decoded = runCatching { ZxingQrFallback.decode(ZxingQrFallback.cameraSource(imageProxy)) }
                    releaseFrame()
                    main.post {
                        val results = decoded.getOrDefault(emptyList()).meaningfulValues()
                        if (results.isEmpty() && failure != null) onError(failure) else onResult(results)
                    }
                }
            } catch (error: Exception) {
                releaseFrame()
                onError(error)
            }
        }
        try {
            val mediaImage = imageProxy.image ?: run { releaseFrame(); return }
            scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) fallbackOrFinish(task.result.mapNotNull(Barcode::getRawValue).meaningfulValues(), null)
                    else fallbackOrFinish(emptyList(), task.exception ?: IllegalStateException("Scanner task cancelled"))
                }
        } catch (error: Exception) {
            releaseFrame()
            main.post { onError(error) }
        }
    }

    override fun scanImage(context: Context, uri: Uri, onResult: (List<String>) -> Unit, onError: (Throwable) -> Unit) {
        // File I/O and bounded software decoding stay off the UI thread. ImageDecoder
        // also applies EXIF orientation and handles transparent gallery images.
        try {
            worker.execute {
                try {
                    val decodedBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val longest = maxOf(info.size.width, info.size.height)
                        if (longest > 2048) decoder.setTargetSize(
                            (info.size.width * 2048L / longest).toInt().coerceAtLeast(1),
                            (info.size.height * 2048L / longest).toInt().coerceAtLeast(1)
                        )
                    }
                    val bitmap = if (decodedBitmap.hasAlpha()) {
                        Bitmap.createBitmap(decodedBitmap.width, decodedBitmap.height, Bitmap.Config.ARGB_8888).also {
                            Canvas(it).apply { drawColor(Color.WHITE); drawBitmap(decodedBitmap, 0f, 0f, null) }
                            decodedBitmap.recycle()
                        }
                    } else decodedBitmap
                    try {
                        scanner.process(InputImage.fromBitmap(bitmap, 0)).addOnCompleteListener { task ->
                            val primary = if (task.isSuccessful) task.result.mapNotNull(Barcode::getRawValue).meaningfulValues() else emptyList()
                            if (primary.isNotEmpty()) {
                                bitmap.recycle()
                                onResult(primary)
                            } else {
                                worker.execute {
                                    val result = runCatching { ZxingQrFallback.decode(bitmap).meaningfulValues() }
                                    bitmap.recycle()
                                    main.post { result.onSuccess(onResult).onFailure(onError) }
                                }
                            }
                        }
                    } catch (error: Exception) {
                        val result = runCatching { ZxingQrFallback.decode(bitmap).meaningfulValues() }
                        bitmap.recycle()
                        main.post { result.onSuccess(onResult).onFailure(onError) }
                    }
                } catch (error: Exception) {
                    main.post { onError(error) }
                }
            }
        } catch (error: Exception) {
            main.post { onError(error) }
        }
    }

    override fun close() {
        scanner.close()
        worker.shutdown()
    }

    private fun List<String>.meaningfulValues(): List<String> = asSequence().map(String::trim)
        .filter { value -> value.any { !it.isWhitespace() && !it.isISOControl() && Character.getType(it) != Character.FORMAT.toInt() } }
        .distinct().toList()
}
