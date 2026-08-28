package com.richardittou.qrcodenow.data.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawContent: String,
    val type: String,
    val title: String,
    val scannedAt: Long,
    val favorite: Boolean,
    val origin: String
)
