package com.richardittou.qrcodenow.presentation.history

import com.google.common.truth.Truth.assertThat
import com.richardittou.qrcodenow.data.database.HistoryEntity
import com.richardittou.qrcodenow.domain.model.QrContent
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import com.richardittou.qrcodenow.domain.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun querySelectionFavoriteAndDeletionUpdateState() = runTest(dispatcher) {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        repository.items.value = listOf(entity(1, "Alfa"), entity(2, "Beta"))
        viewModel.setQuery("beta")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.items.map { it.id }).containsExactly(2L)

        viewModel.toggleSelection(2)
        viewModel.deleteSelected()
        advanceUntilIdle()
        assertThat(repository.items.value.map { it.id }).containsExactly(1L)

        repository.items.value = listOf(entity(3, "Favorito"))
        viewModel.toggleFavorite(repository.items.value.single())
        advanceUntilIdle()
        assertThat(repository.items.value.single().favorite).isTrue()
    }

    private fun entity(id: Long, title: String) = HistoryEntity(
        id = id,
        rawContent = title,
        type = "TEXT",
        title = title,
        scannedAt = id,
        favorite = false,
        origin = "CAMERA"
    )
}

private class FakeHistoryRepository : HistoryRepository {
    val items = MutableStateFlow<List<HistoryEntity>>(emptyList())

    override fun observe(query: String, favoritesOnly: Boolean): Flow<List<HistoryEntity>> = items.map { values ->
        values.filter { (!favoritesOnly || it.favorite) && (query.isBlank() || it.rawContent.contains(query, ignoreCase = true)) }
    }

    override suspend fun add(content: QrContent, origin: ScanOrigin, favorite: Boolean): Long = 0

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(favorite = favorite) else it }
    }

    override suspend fun delete(ids: Set<Long>) {
        items.value = items.value.filterNot { it.id in ids }
    }

    override suspend fun snapshot(): List<HistoryEntity> = items.value
    override suspend fun restore(items: List<HistoryEntity>) { this.items.value = this.items.value + items }
    override suspend fun clear() { items.value = emptyList() }
}
