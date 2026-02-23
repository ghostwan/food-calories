package com.ghostwan.snapcal.domain.repository

import kotlinx.coroutines.flow.Flow

interface DailyNoteRepository {
    fun getNoteForDate(date: String): Flow<String?>
    suspend fun saveNote(date: String, note: String)
    suspend fun deleteNote(date: String)
    suspend fun getAllNotes(): List<Pair<String, String>>
    suspend fun saveNotes(notes: List<Pair<String, String>>)
}
