package ru.student.ocrscanner.recognition

import android.graphics.Rect

/** Один блок распознанного текста с координатами и уверенностью. */
data class TextBlock(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float
)

/** Результат распознавания изображения. */
data class RecognitionResult(
    val fullText: String,
    val averageConfidence: Float,
    val blocks: List<TextBlock>
)