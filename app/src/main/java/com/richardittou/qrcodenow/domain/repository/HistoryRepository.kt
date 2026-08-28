package com.richardittou.qrcodenow.domain.repository

import com.richardittou.qrcodenow.data.database.HistoryEntity
import com.richardittou.qrcodenow.domain.model.QrContent
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observe(query: String = "", favoritesOnly: Boolean = false): Flow<List<HistoryEntity>>
    suspend fun add(content: QrContent, origin: ScanOrigin, favorite: Boolean = false): Long
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun delete(ids: Set<Long>)
    suspend fun snapshot(): List<HistoryEntity>
    suspend fun restore(items: List<HistoryEntity>)
    suspend fun clear()
}
