package com.richardittou.qrcodenow.data.scanner

import android.content.Context
import android.net.Uri
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.richardittou.qrcodenow.domain.scanner.QrScannerEngine
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlKitQrScannerEngine @Inject constructor() : QrScannerEngine {
    private val busy = AtomicBoolean(false)
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy, onResult: (List<String>) -> Unit, onError: (Throwable) -> Unit) {
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            busy.set(false)
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { codes -> onResult(codes.meaningfulRawValues()) }
            .addOnFailureListener(onError)
            .addOnCompleteListener {
                busy.set(false)
                imageProxy.close()
            }
    }

    override fun scanImage(context: Context, uri: Uri, onResult: (List<String>) -> Unit, onError: (Throwable) -> Unit) {
        runCatching { InputImage.fromFilePath(context, uri) }
            .onSuccess { image ->
                scanner.process(image)
                    .addOnSuccessListener { codes -> onResult(codes.meaningfulRawValues()) }
                    .addOnFailureListener(onError)
            }
            .onFailure(onError)
    }

    override fun close() = scanner.close()

    private fun List<Barcode>.meaningfulRawValues(): List<String> = asSequence()
        .mapNotNull(Barcode::getRawValue)
        .map(String::trim)
        .filter { it.isMeaningfulQrValue() }
        .distinct()
        .toList()

    private fun String.isMeaningfulQrValue(): Boolean = any { character ->
        !character.isWhitespace() &&
            !character.isISOControl() &&
            Character.getType(character) != Character.FORMAT.toInt()
    }
}
