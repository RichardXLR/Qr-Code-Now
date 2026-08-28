package com.richardittou.qrcodenow.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richardittou.qrcodenow.domain.model.AppSettings
import com.richardittou.qrcodenow.domain.model.AppTheme
import com.richardittou.qrcodenow.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    fun setTheme(value: AppTheme) = viewModelScope.launch { repository.setTheme(value) }
    fun setVibration(value: Boolean) = viewModelScope.launch { repository.setVibration(value) }
    fun setAutoOpen(value: Boolean) = viewModelScope.launch { repository.setAutoOpenSafeUrls(value) }
}
