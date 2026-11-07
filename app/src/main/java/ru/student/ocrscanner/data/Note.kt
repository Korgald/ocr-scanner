package ru.student.ocrscanner.data

import ru.student.ocrscanner.data.local.NoteEntity

data class Note(
    val id: Long = 0,
    val title: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val averageConfidence: Float? = null
)

internal fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    averageConfidence = averageConfidence
)

internal fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    averageConfidence = averageConfidence
)
