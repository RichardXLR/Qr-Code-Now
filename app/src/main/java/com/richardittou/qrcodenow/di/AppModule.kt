package com.richardittou.qrcodenow.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.richardittou.qrcodenow.data.database.HistoryDao
import com.richardittou.qrcodenow.data.database.HistoryDatabase
import com.richardittou.qrcodenow.data.repository.HistoryRepositoryImpl
import com.richardittou.qrcodenow.data.repository.SettingsRepositoryImpl
import com.richardittou.qrcodenow.domain.generator.QrEncoder
import com.richardittou.qrcodenow.domain.generator.ZxingQrEncoder
import com.richardittou.qrcodenow.domain.parser.DefaultQrContentParser
import com.richardittou.qrcodenow.domain.parser.QrContentParser
import com.richardittou.qrcodenow.domain.repository.HistoryRepository
import com.richardittou.qrcodenow.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
    @Binds abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun bindParser(impl: DefaultQrContentParser): QrContentParser
    @Binds abstract fun bindEncoder(impl: ZxingQrEncoder): QrEncoder
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HistoryDatabase =
        Room.databaseBuilder(context, HistoryDatabase::class.java, "qr_code_now.db")
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Provides fun provideHistoryDao(database: HistoryDatabase): HistoryDao = database.historyDao()
}
