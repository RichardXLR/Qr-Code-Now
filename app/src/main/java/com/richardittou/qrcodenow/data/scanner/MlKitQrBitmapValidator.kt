package com.richardittou.qrcodenow.data.scanner

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.richardittou.qrcodenow.domain.generator.QrBitmapValidator
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MlKitQrBitmapValidator @Inject constructor() : QrBitmapValidator {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    override suspend fun isReadable(bitmap: Bitmap, expectedContent: String): Boolean {
        if (!scan(bitmap).contains(expectedContent)) return false
        if (bitmap.width <= PREVIEW_VALIDATION_SIZE && bitmap.height <= PREVIEW_VALIDATION_SIZE) return true

        val preview = Bitmap.createScaledBitmap(
            bitmap,
            PREVIEW_VALIDATION_SIZE,
            PREVIEW_VALIDATION_SIZE,
            false
        )
        // ML Kit may still own the pixels after coroutine cancellation. Let the
        // bitmap be collected after the task, rather than recycling it early.
        return scan(preview).contains(expectedContent)
    }

    private suspend fun scan(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { continuation ->
        try {
            scanner.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { codes ->
                    if (continuation.isActive) {
                        continuation.resume(codes.mapNotNull(Barcode::getRawValue))
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        } catch (error: Throwable) {
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

    companion object {
        private const val PREVIEW_VALIDATION_SIZE = 512
    }
}
