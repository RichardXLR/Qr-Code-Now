package com.richardittou.qrcodenow.presentation.generator

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richardittou.qrcodenow.domain.generator.GeneratorInput
import com.richardittou.qrcodenow.domain.generator.QrEncoder
import com.richardittou.qrcodenow.domain.generator.QrPayloadFactory
import com.richardittou.qrcodenow.domain.model.QrContent
import com.richardittou.qrcodenow.domain.model.QrType
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import com.richardittou.qrcodenow.domain.parser.QrContentParser
import com.richardittou.qrcodenow.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GeneratorUiState(
    val type: QrType = QrType.TEXT,
    val primary: String = "",
    val secondary: String = "",
    val tertiary: String = "",
    val extra: String = "",
    val payload: String? = null,
    val bitmap: Bitmap? = null,
    val historyId: Long? = null,
    val isFavorite: Boolean = false,
    val error: String? = null,
    val generating: Boolean = false
)

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val encoder: QrEncoder,
    private val parser: QrContentParser,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState = _uiState.asStateFlow()

    fun setType(type: QrType) {
        _uiState.value = GeneratorUiState(type = type, tertiary = if (type == QrType.WIFI) "WPA2" else "")
    }
    fun setPrimary(value: String) { _uiState.value = _uiState.value.invalidated(primary = value.take(16_384)) }
    fun setSecondary(value: String) { _uiState.value = _uiState.value.invalidated(secondary = value.take(4_096)) }
    fun setTertiary(value: String) { _uiState.value = _uiState.value.invalidated(tertiary = value.take(4_096)) }
    fun setExtra(value: String) { _uiState.value = _uiState.value.invalidated(extra = value.take(4_096)) }

    fun generate() {
        val state = _uiState.value
        val payload = QrPayloadFactory.build(GeneratorInput(state.type, state.primary, state.secondary, state.tertiary, state.extra))
        if (payload == null) {
            _uiState.value = state.copy(error = "Confira os campos e os formatos informados.")
            return
        }
        _uiState.value = state.copy(generating = true, error = null, bitmap = null, payload = null, historyId = null, isFavorite = false)
        viewModelScope.launch {
            runCatching {
                val bitmap = withContext(Dispatchers.Default) { encoder.encode(payload) }
                if (!_uiState.value.matchesInput(state)) return@launch
                val id = historyRepository.add(parser.parse(payload), ScanOrigin.GENERATED)
                bitmap to id
            }
                .onSuccess { (bitmap, id) ->
                    if (_uiState.value.matchesInput(state)) {
                        _uiState.value = _uiState.value.copy(payload = payload, bitmap = bitmap, historyId = id, generating = false)
                    }
                }
                .onFailure {
                    if (_uiState.value.matchesInput(state)) {
                        _uiState.value = _uiState.value.copy(error = "Não foi possível gerar o QR Code.", generating = false)
                    }
                }
        }
    }

    fun addToFavorites() {
        val payload = _uiState.value.payload ?: return
        viewModelScope.launch {
            runCatching {
                val currentId = _uiState.value.historyId
                if (currentId != null) {
                    historyRepository.setFavorite(currentId, true)
                    currentId
                } else {
                    val content: QrContent = parser.parse(payload)
                    historyRepository.add(content, ScanOrigin.GENERATED, favorite = true)
                }
            }.onSuccess { id ->
                _uiState.value = _uiState.value.copy(historyId = id, isFavorite = true)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = "Não foi possível adicionar aos favoritos.")
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    private fun GeneratorUiState.invalidated(
        primary: String = this.primary,
        secondary: String = this.secondary,
        tertiary: String = this.tertiary,
        extra: String = this.extra
    ) = copy(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        extra = extra,
        bitmap = null,
        payload = null,
        historyId = null,
        isFavorite = false,
        error = null,
        generating = false
    )

    private fun GeneratorUiState.matchesInput(other: GeneratorUiState): Boolean =
        type == other.type && primary == other.primary && secondary == other.secondary &&
            tertiary == other.tertiary && extra == other.extra
}
