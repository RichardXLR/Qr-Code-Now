package com.richardittou.qrcodenow.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richardittou.qrcodenow.data.database.HistoryEntity
import com.richardittou.qrcodenow.domain.model.QrType
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import com.richardittou.qrcodenow.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items: List<HistoryEntity> = emptyList(),
    val query: String = "",
    val selectedIds: Set<Long> = emptySet(),
    val selectionMode: Boolean = false,
    val typeFilter: QrType? = null,
    val originFilter: ScanOrigin? = null,
    val oldestFirst: Boolean = false
)

data class HistoryDeletion(val items: List<HistoryEntity>)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: HistoryRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val favoritesOnly = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectionMode = MutableStateFlow(false)
    private val typeFilter = MutableStateFlow<QrType?>(null)
    private val originFilter = MutableStateFlow<ScanOrigin?>(null)
    private val oldestFirst = MutableStateFlow(false)
    private val _deletions = MutableSharedFlow<HistoryDeletion>(extraBufferCapacity = 1)
    val deletions = _deletions.asSharedFlow()

    private val items = combine(query, favoritesOnly) { q, favorites -> q to favorites }
        .flatMapLatest { (q, favorites) -> repository.observe(q, favorites) }

    private val filteredItems = combine(items, typeFilter, originFilter, oldestFirst) { history, type, origin, oldest ->
        history.asSequence()
            .filter { type == null || it.type == type.name }
            .filter { origin == null || it.origin == origin.name }
            .let { sequence -> if (oldest) sequence.sortedBy { it.scannedAt } else sequence.sortedByDescending { it.scannedAt } }
            .toList()
    }

    val uiState = combine(filteredItems, query, selectedIds, selectionMode) { history, q, selected, selecting ->
        HistoryUiState(history, q, selected, selecting, typeFilter.value, originFilter.value, oldestFirst.value)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFavoritesOnly(value: Boolean) { favoritesOnly.value = value }
    fun setQuery(value: String) { query.value = value.take(200) }
    fun setTypeFilter(value: QrType?) { typeFilter.value = value; clearSelection() }
    fun setOriginFilter(value: ScanOrigin?) { originFilter.value = value; clearSelection() }
    fun toggleSort() { oldestFirst.value = !oldestFirst.value; clearSelection() }
    fun beginSelection() { selectionMode.value = true }
    fun toggleSelection(id: Long) {
        selectionMode.value = true
        selectedIds.value = selectedIds.value.toMutableSet().apply { if (!add(id)) remove(id) }
    }
    fun selectAll() { selectionMode.value = true; selectedIds.value = uiState.value.items.mapTo(mutableSetOf()) { it.id } }
    fun clearSelection() { selectedIds.value = emptySet(); selectionMode.value = false }
    fun toggleFavorite(item: HistoryEntity) = viewModelScope.launch { repository.setFavorite(item.id, !item.favorite) }
    fun deleteSelected() = viewModelScope.launch {
        val ids = selectedIds.value
        val removed = uiState.value.items.filter { it.id in ids }
        repository.delete(ids)
        clearSelection()
        if (removed.isNotEmpty()) _deletions.emit(HistoryDeletion(removed))
    }
    fun delete(id: Long) = viewModelScope.launch {
        val removed = uiState.value.items.filter { it.id == id }
        repository.delete(setOf(id))
        if (removed.isNotEmpty()) _deletions.emit(HistoryDeletion(removed))
    }
    fun clearHistory() = viewModelScope.launch {
        val removed = repository.snapshot()
        repository.clear()
        clearSelection()
        if (removed.isNotEmpty()) _deletions.emit(HistoryDeletion(removed))
    }
    fun undo(deletion: HistoryDeletion) = viewModelScope.launch { repository.restore(deletion.items) }
}
