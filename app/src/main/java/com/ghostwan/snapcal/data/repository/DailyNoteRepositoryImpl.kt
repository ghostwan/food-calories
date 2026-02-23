package com.ghostwan.snapcal.data.repository

import com.ghostwan.snapcal.data.local.DailyNoteDao
import com.ghostwan.snapcal.data.local.DailyNoteEntity
import com.ghostwan.snapcal.domain.repository.DailyNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DailyNoteRepositoryImpl(private val dao: DailyNoteDao) : DailyNoteRepository {

    override fun getNoteForDate(date: String): Flow<String?> {
        return dao.getByDate(date).map { it?.note }
    }

    override suspend fun saveNote(date: String, note: String) {
        dao.upsert(DailyNoteEntity(date = date, note = note))
    }

    override suspend fun deleteNote(date: String) {
        dao.delete(date)
    }

    override suspend fun getAllNotes(): List<Pair<String, String>> {
        return dao.getAll().map { it.date to it.note }
    }

    override suspend fun saveNotes(notes: List<Pair<String, String>>) {
        notes.forEach { (date, note) ->
            dao.upsert(DailyNoteEntity(date = date, note = note))
        }
    }
}
