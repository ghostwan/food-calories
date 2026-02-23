package com.ghostwan.snapcal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_notes")
data class DailyNoteEntity(
    @PrimaryKey val date: String,
    val note: String
)
