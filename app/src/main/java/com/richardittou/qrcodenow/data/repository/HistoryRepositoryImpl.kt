package com.richardittou.qrcodenow.data.repository

import com.richardittou.qrcodenow.data.database.HistoryDao
import com.richardittou.qrcodenow.data.database.HistoryEntity
import com.richardittou.qrcodenow.domain.model.QrContent
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import com.richardittou.qrcodenow.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(private val dao: HistoryDao) : HistoryRepository {
    override fun observe(query: String, favoritesOnly: Boolean): Flow<List<HistoryEntity>> = when {
        query.isBlank() && favoritesOnly -> dao.observeFavorites()
        query.isBlank() -> dao.observeAll()
        favoritesOnly -> dao.searchFavorites(query.trim())
        else -> dao.searchAll(query.trim())
    }

    override suspend fun add(content: QrContent, origin: ScanOrigin, favorite: Boolean): Long = dao.insert(
        HistoryEntity(
            rawContent = content.raw,
            type = content.type.name,
            title = content.title,
            scannedAt = System.currentTimeMillis(),
            favorite = favorite,
            origin = origin.name
        )
    )

    override suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    override suspend fun delete(ids: Set<Long>) { if (ids.isNotEmpty()) dao.deleteByIds(ids.toList()) }
    override suspend fun snapshot(): List<HistoryEntity> = dao.getAll()
    override suspend fun restore(items: List<HistoryEntity>) { if (items.isNotEmpty()) dao.insertAll(items) }
    override suspend fun clear() = dao.clear()
}
