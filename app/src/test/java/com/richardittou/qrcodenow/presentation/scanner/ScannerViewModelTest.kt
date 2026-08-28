package com.richardittou.qrcodenow.presentation.scanner

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import com.google.common.truth.Truth.assertThat
import com.richardittou.qrcodenow.data.database.HistoryEntity
import com.richardittou.qrcodenow.domain.model.AppSettings
import com.richardittou.qrcodenow.domain.model.AppTheme
import com.richardittou.qrcodenow.domain.model.QrContent
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import com.richardittou.qrcodenow.domain.parser.QrContentParser
import com.richardittou.qrcodenow.domain.repository.HistoryRepository
import com.richardittou.qrcodenow.domain.repository.SettingsRepository
import com.richardittou.qrcodenow.domain.scanner.QrScannerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun emptyWhitespaceAndInvisibleDetectionsAreIgnored() = runTest(dispatcher) {
        val history = RecordingHistoryRepository()
        val parser = RecordingParser()
        val viewModel = ScannerViewModel(parser, history, FakeSettingsRepository(), FakeScannerEngine())

        viewModel.onDetected(listOf("", "   \n", "\u200B"), ScanOrigin.CAMERA)

        assertThat(viewModel.uiState.value.analysisPaused).isFalse()
        assertThat(viewModel.uiState.value.selected).isNull()
        assertThat(parser.calls).isEqualTo(0)
        assertThat(history.addCalls).isEqualTo(0)
    }

    @Test
    fun duplicateCameraReadingInsideWindowIsSavedOnlyOnce() = runTest(dispatcher) {
        val history = RecordingHistoryRepository()
        val viewModel = ScannerViewModel(RecordingParser(), history, FakeSettingsRepository(), FakeScannerEngine())

        viewModel.onDetected(listOf("conteúdo real"), ScanOrigin.CAMERA)
        advanceUntilIdle()
        viewModel.dismissResult()
        viewModel.onDetected(listOf("conteúdo real"), ScanOrigin.CAMERA)
        advanceUntilIdle()

        assertThat(history.addCalls).isEqualTo(1)
    }
}

private class RecordingParser : QrContentParser {
    var calls = 0
    override fun parse(rawValue: String): QrContent {
        calls++
        return QrContent.Text(rawValue)
    }
}

private class RecordingHistoryRepository : HistoryRepository {
    var addCalls = 0
    override fun observe(query: String, favoritesOnly: Boolean): Flow<List<HistoryEntity>> = MutableStateFlow(emptyList())
    override suspend fun add(content: QrContent, origin: ScanOrigin, favorite: Boolean): Long { addCalls++; return 1 }
    override suspend fun setFavorite(id: Long, favorite: Boolean) = Unit
    override suspend fun delete(ids: Set<Long>) = Unit
    override suspend fun snapshot(): List<HistoryEntity> = emptyList()
    override suspend fun restore(items: List<HistoryEntity>) = Unit
    override suspend fun clear() = Unit
}

private class FakeSettingsRepository : SettingsRepository {
    override val settings: Flow<AppSettings> = MutableStateFlow(AppSettings())
    override suspend fun setTheme(theme: AppTheme) = Unit
    override suspend fun setVibration(enabled: Boolean) = Unit
    override suspend fun setAutoOpenSafeUrls(enabled: Boolean) = Unit
}

private class FakeScannerEngine : QrScannerEngine {
    override fun analyze(imageProxy: ImageProxy, onResult: (List<String>) -> Unit, onError: (Throwable) -> Unit) = Unit
    override fun scanImage(context: Context, uri: Uri, onResult: (List<String>) -> Unit, onError: (Throwable) -> Unit) = Unit
    override fun close() = Unit
}
