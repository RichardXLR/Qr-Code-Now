package com.richardittou.qrcodenow.data.database

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = true)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
