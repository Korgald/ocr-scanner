package ru.student.ocrscanner.recognition

import android.graphics.Bitmap
import ru.student.ocrscanner.data.settings.OcrLanguage

/** Репозиторий распознавания текста через ML Kit. */
interface RecognitionRepository {
    /** Распознать текст на Bitmap с учётом выбранного языка. */
    suspend fun recognize(bitmap: Bitmap, language: OcrLanguage): RecognitionResult?
}
