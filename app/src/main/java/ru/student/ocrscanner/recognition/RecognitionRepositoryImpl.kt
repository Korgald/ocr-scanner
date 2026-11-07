package ru.student.ocrscanner.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.student.ocrscanner.data.settings.OcrLanguage
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tesseract4Android OCR implementation with light post-cleanup:
 * trims lines, drops empty ones, collapses runs of spaces and blank lines,
 * so recognized text does not look scattered.
 */
@Singleton
class RecognitionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RecognitionRepository {

    companion object {
        private const val DATA_DIR = "tessdata"
        private const val PSM_SINGLE_BLOCK = 6
    }

    override suspend fun recognize(bitmap: Bitmap, language: OcrLanguage): RecognitionResult? =
        withContext(Dispatchers.Default) {
            val tessLang = when (language) {
                OcrLanguage.RUSSIAN -> "rus"
                OcrLanguage.LATIN -> "eng"
                OcrLanguage.AUTO -> "eng+rus"
            }

            val dataPath = File(context.filesDir, DATA_DIR)
            if (!dataPath.exists()) dataPath.mkdirs()
            copyAssetIfMissing("eng.traineddata", File(dataPath, "eng.traineddata"))
            copyAssetIfMissing("rus.traineddata", File(dataPath, "rus.traineddata"))

            val tessApi = TessBaseAPI()
            if (!tessApi.init(context.filesDir.absolutePath, tessLang)) {
                tessApi.recycle()
                return@withContext null
            }

            try {
                tessApi.setPageSegMode(PSM_SINGLE_BLOCK)
                tessApi.setVariable("user_defined_dpi", "300")
                tessApi.setVariable("preserve_interword_spaces", "1")

                tessApi.setImage(bitmap)
                val raw = tessApi.utF8Text ?: ""
                val text = cleanText(raw)
                val meanConfidence = tessApi.meanConfidence() / 100f

                val blocks = if (text.isNotBlank()) {
                    listOf(
                        TextBlock(
                            text = text,
                            boundingBox = Rect(0, 0, bitmap.width, bitmap.height),
                            confidence = meanConfidence
                        )
                    )
                } else {
                    emptyList()
                }

                RecognitionResult(
                    fullText = text,
                    averageConfidence = meanConfidence,
                    blocks = blocks
                )
            } catch (e: Exception) {
                null
            } finally {
                tessApi.recycle()
            }
        }

    /** Trim lines, drop empties, collapse space runs and 3+ newlines. */
    private fun cleanText(raw: String): String =
        raw.lineSequence()
            .map { line -> line.replace(Regex("[ \\t]{2,}"), " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")

    private fun copyAssetIfMissing(assetName: String, destFile: File) {
        if (destFile.exists()) return
        context.assets.open(DATA_DIR + "/" + assetName).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}