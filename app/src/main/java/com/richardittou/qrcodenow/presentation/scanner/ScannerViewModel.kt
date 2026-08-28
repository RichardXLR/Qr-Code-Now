package com.richardittou.qrcodenow.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richardittou.qrcodenow.domain.model.QrContent
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import com.richardittou.qrcodenow.domain.parser.QrContentParser
import com.richardittou.qrcodenow.domain.repository.HistoryRepository
import com.richardittou.qrcodenow.domain.repository.SettingsRepository
import com.richardittou.qrcodenow.domain.scanner.QrScannerEngine
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.security.MessageDigest

data class ScannerUiState(
    val results: List<QrContent> = emptyList(),
    val selected: QrContent? = null,
    val origin: ScanOrigin = ScanOrigin.CAMERA,
    val error: String? = null,
    val analysisPaused: Boolean = false
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val parser: QrContentParser,
    private val historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
    private val scannerEngine: QrScannerEngine
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState = _uiState.asStateFlow()
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.richardittou.qrcodenow.domain.model.AppSettings())
    private val _feedback = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()
    private var lastFingerprint: ByteArray? = null
    private var lastDetectedAt = 0L

    fun onDetected(values: List<String>, origin: ScanOrigin) {
        val meaningfulValues = values.asSequence()
            .map(String::trim)
            .filter { value -> value.any { it.isMeaningfulQrCharacter() } }
            .distinct()
            .toList()
        if (meaningfulValues.isEmpty()) {
            if (origin == ScanOrigin.GALLERY) _uiState.value = _uiState.value.copy(error = "Nenhum QR Code foi encontrado nessa imagem.")
            return
        }
        val parsed = meaningfulValues.map(parser::parse).filter { it.raw.isNotBlank() }.distinctBy { it.raw }
        if (parsed.isEmpty()) return
        val now = System.currentTimeMillis()
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(parsed.map { it.raw }.sorted().joinToString("\u0000").toByteArray())
        if (origin == ScanOrigin.CAMERA && lastFingerprint?.contentEquals(fingerprint) == true && now - lastDetectedAt < DUPLICATE_WINDOW_MS) return
        lastFingerprint = fingerprint
        lastDetectedAt = now
        val selected = parsed.singleOrNull()
        _uiState.value = ScannerUiState(parsed, selected, origin, analysisPaused = true)
        _feedback.tryEmit(Unit)
        if (selected != null) save(selected, origin)
    }

    fun select(content: QrContent) {
        _uiState.value = _uiState.value.copy(selected = content)
        save(content, _uiState.value.origin)
    }

    fun dismissResult() { _uiState.value = ScannerUiState() }
    fun showError(message: String) { _uiState.value = _uiState.value.copy(error = message) }
    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    fun analyze(imageProxy: ImageProxy) {
        if (_uiState.value.analysisPaused) {
            imageProxy.close()
            return
        }
        scannerEngine.analyze(
            imageProxy,
            onResult = { onDetected(it, ScanOrigin.CAMERA) },
            onError = { showError("Não foi possível analisar a imagem da câmera.") }
        )
    }

    fun scanImage(context: Context, uri: Uri) {
        scannerEngine.scanImage(
            context,
            uri,
            onResult = { onDetected(it, ScanOrigin.GALLERY) },
            onError = { showError("Não foi possível abrir ou processar essa imagem.") }
        )
    }

    private fun save(content: QrContent, origin: ScanOrigin) {
        viewModelScope.launch { historyRepository.add(content, origin) }
    }

    private fun Char.isMeaningfulQrCharacter(): Boolean =
        !isWhitespace() && !isISOControl() && Character.getType(this) != Character.FORMAT.toInt()

    companion object { private const val DUPLICATE_WINDOW_MS = 1_500L }
}
