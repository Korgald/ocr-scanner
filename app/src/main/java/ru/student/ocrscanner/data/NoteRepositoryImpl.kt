package ru.student.ocrscanner.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.student.ocrscanner.data.local.NoteDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override val notes: Flow<List<Note>> =
        noteDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getNote(id: Long): Note? =
        noteDao.getById(id)?.toDomain()

    override suspend fun saveNote(note: Note): Long {
        val entity = note.toEntity()
        return if (note.id == 0L) {
            noteDao.insert(entity)
        } else {
            noteDao.update(entity)
            note.id
        }
    }

    override suspend fun deleteNote(id: Long) {
        noteDao.deleteById(id)
    }

    override fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.search(query.trim()).map { entities -> entities.map { it.toDomain() } }
}
