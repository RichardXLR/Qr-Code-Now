package com.richardittou.qrcodenow.di

import com.richardittou.qrcodenow.data.scanner.MlKitQrScannerEngine
import com.richardittou.qrcodenow.domain.action.AndroidExternalActionLauncher
import com.richardittou.qrcodenow.domain.action.ExternalActionLauncher
import com.richardittou.qrcodenow.domain.scanner.QrScannerEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ScannerBindingsModule {
    @Binds abstract fun bindScanner(impl: MlKitQrScannerEngine): QrScannerEngine
}

@Module
@InstallIn(SingletonComponent::class)
object ActionModule {
    @Provides fun provideExternalActionLauncher(): ExternalActionLauncher = AndroidExternalActionLauncher()
}
