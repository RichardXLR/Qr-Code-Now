package com.richardittou.qrcodenow.domain.generator

import android.graphics.Bitmap

interface QrBitmapValidator {
    suspend fun isReadable(bitmap: Bitmap, expectedContent: String): Boolean
}
