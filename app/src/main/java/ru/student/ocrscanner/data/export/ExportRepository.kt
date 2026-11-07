package ru.student.ocrscanner.data.export

import android.net.Uri

/** Репозиторий экспорта заметок во внешние форматы. */
interface ExportRepository {
    /** Записать текст заметки в Uri как TXT. */
    suspend fun exportTxt(uri: Uri, title: String, text: String): Boolean

    /** Записать заметку в Uri как PDF-документ. */
    suspend fun exportPdf(uri: Uri, title: String, text: String): Boolean
}
