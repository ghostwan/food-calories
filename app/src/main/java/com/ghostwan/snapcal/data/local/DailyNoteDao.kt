package com.ghostwan.snapcal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNoteDao {

    @Query("SELECT * FROM daily_notes WHERE date = :date LIMIT 1")
    fun getByDate(date: String): Flow<DailyNoteEntity?>

    @Upsert
    suspend fun upsert(note: DailyNoteEntity)

    @Query("DELETE FROM daily_notes WHERE date = :date")
    suspend fun delete(date: String)

    @Query("SELECT * FROM daily_notes ORDER BY date ASC")
    suspend fun getAll(): List<DailyNoteEntity>
}
