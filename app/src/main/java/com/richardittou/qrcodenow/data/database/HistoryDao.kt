package com.richardittou.qrcodenow.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY scannedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE favorite = 1 ORDER BY scannedAt DESC")
    fun observeFavorites(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE instr(lower(title), lower(:query)) > 0 OR instr(lower(rawContent), lower(:query)) > 0 ORDER BY scannedAt DESC")
    fun searchAll(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE favorite = 1 AND (instr(lower(title), lower(:query)) > 0 OR instr(lower(rawContent), lower(:query)) > 0) ORDER BY scannedAt DESC")
    fun searchFavorites(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HistoryEntity>)

    @Query("SELECT * FROM history ORDER BY scannedAt DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("UPDATE history SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM history")
    suspend fun clear()
}
