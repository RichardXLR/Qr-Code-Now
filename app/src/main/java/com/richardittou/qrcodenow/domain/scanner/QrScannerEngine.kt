package com.richardittou.qrcodenow.domain.scanner

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy

interface QrScannerEngine {
    fun analyze(imageProxy: ImageProxy, onResult: (List<String>) -> Unit, onError: (Throwable) -> Unit)
    fun scanImage(context: Context, uri: Uri, onResult: (List<String>) -> Unit, onError: (Throwable) -> Unit)
    fun close()
}
