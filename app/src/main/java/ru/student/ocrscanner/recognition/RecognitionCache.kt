package ru.student.ocrscanner.recognition

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache to move big bitmaps/text between screens without
 * stuffing them into navigation arguments.
 */
@Singleton
class RecognitionCache @Inject constructor() {
    /** Full-res decoded image waiting to be cropped. */
    var sourceBitmap: Bitmap? = null
    /** Processed (binarized) image that was actually recognized. */
    var bitmap: Bitmap? = null
    var result: RecognitionResult? = null
    var editedText: String? = null

    fun clear() {
        sourceBitmap = null
        bitmap = null
        result = null
        editedText = null
    }
}