package com.richardittou.qrcodenow.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.richardittou.qrcodenow.domain.model.AppSettings
import com.richardittou.qrcodenow.domain.model.AppTheme
import com.richardittou.qrcodenow.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepository {
    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            theme = runCatching { AppTheme.valueOf(prefs[THEME] ?: AppTheme.SYSTEM.name) }.getOrDefault(AppTheme.SYSTEM),
            vibrationEnabled = prefs[VIBRATION] ?: true,
            autoOpenSafeUrls = prefs[AUTO_OPEN] ?: false
        )
    }

    override suspend fun setTheme(theme: AppTheme) { context.settingsDataStore.edit { it[THEME] = theme.name } }
    override suspend fun setVibration(enabled: Boolean) { context.settingsDataStore.edit { it[VIBRATION] = enabled } }
    override suspend fun setAutoOpenSafeUrls(enabled: Boolean) { context.settingsDataStore.edit { it[AUTO_OPEN] = enabled } }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val VIBRATION = booleanPreferencesKey("vibration")
        val AUTO_OPEN = booleanPreferencesKey("auto_open_safe_urls")
    }
}
