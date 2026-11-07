package ru.student.ocrscanner.data

import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    val notes: Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun saveNote(note: Note): Long
    suspend fun deleteNote(id: Long)
    fun searchNotes(query: String): Flow<List<Note>>
}
