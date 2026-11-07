package ru.student.ocrscanner.ui.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.student.ocrscanner.camera.ImagePreprocessor
import ru.student.ocrscanner.data.settings.SettingsRepository
import ru.student.ocrscanner.recognition.RecognitionCache
import ru.student.ocrscanner.recognition.RecognitionRepository
import ru.student.ocrscanner.ui.scan.ScanViewModel
import java.io.File
import javax.inject.Inject

// Normalized crop rect (0..1), valid at any container size / after restart.
data class NormRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class CropUiState(
    val isProcessing: Boolean = false,
    val done: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CropViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val imagePreprocessor: ImagePreprocessor,
    private val recognitionRepository: RecognitionRepository,
    private val settingsRepository: SettingsRepository,
    private val recognitionCache: RecognitionCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropUiState())
    val uiState: StateFlow<CropUiState> = _uiState.asStateFlow()

    private val _bitmap = MutableStateFlow<Bitmap?>(recognitionCache.sourceBitmap)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    private val _rect = MutableStateFlow(restoreRect())
    val rect: StateFlow<NormRect?> = _rect.asStateFlow()

    init {
        if (recognitionCache.sourceBitmap == null) {
            viewModelScope.launch(Dispatchers.IO) {
                val f = File(context.filesDir, ScanViewModel.SRC_FILE)
                if (f.exists()) {
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    if (bmp != null) {
                        recognitionCache.sourceBitmap = bmp
                        _bitmap.value = bmp
                    }
                }
            }
        }
    }

    fun setRect(nr: NormRect?) {
        _rect.value = nr
        if (nr == null) {
            savedStateHandle[KEY_L] = null as Float?
            savedStateHandle[KEY_T] = null as Float?
            savedStateHandle[KEY_R] = null as Float?
            savedStateHandle[KEY_B] = null as Float?
        } else {
            savedStateHandle[KEY_L] = nr.left
            savedStateHandle[KEY_T] = nr.top
            savedStateHandle[KEY_R] = nr.right
            savedStateHandle[KEY_B] = nr.bottom
        }
    }

    // UI converts normalized rect -> source pixels (it knows box size) and calls this.
    fun confirmCropSource(rect: IntRect) {
        val src = _bitmap.value ?: recognitionCache.sourceBitmap
        if (src == null) {
            _uiState.update { it.copy(errorMessage = "Нет изображения для кропа") }
            return
        }
        val left = rect.left.coerceIn(0, src.width - 1)
        val top = rect.top.coerceIn(0, src.height - 1)
        val right = rect.right.coerceIn(left + 1, src.width)
        val bottom = rect.bottom.coerceIn(top + 1, src.height)
        val w = right - left
        val h = bottom - top

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val cropped = try { Bitmap.createBitmap(src, left, top, w, h) } catch (e: Exception) { null }
            if (cropped == null) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Не удалось вырезать область") }
                return@launch
            }
            val processed = imagePreprocessor.prepareForOcr(cropped)
            if (processed == null) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Не удалось обработать область") }
                return@launch
            }
            val lang = settingsRepository.settings.first().ocrLanguage
            val result = try { recognitionRepository.recognize(processed, lang) } catch (e: Exception) { null }
            if (result == null) {
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Не удалось распознать текст") }
                return@launch
            }
            recognitionCache.bitmap = processed
            recognitionCache.result = result
            recognitionCache.editedText = result.fullText
            recognitionCache.sourceBitmap = null
            runCatching { File(context.filesDir, ScanViewModel.SRC_FILE).delete() }
            _uiState.update { it.copy(isProcessing = false, done = true) }
        }
    }

    fun cancel() {
        recognitionCache.sourceBitmap = null
        runCatching { File(context.filesDir, ScanViewModel.SRC_FILE).delete() }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    private fun restoreRect(): NormRect? {
        val l = savedStateHandle[KEY_L] as? Float ?: return null
        val t = savedStateHandle[KEY_T] as? Float ?: return null
        val r = savedStateHandle[KEY_R] as? Float ?: return null
        val b = savedStateHandle[KEY_B] as? Float ?: return null
        return NormRect(l, t, r, b)
    }

    companion object {
        private const val KEY_L = "rect_left"
        private const val KEY_T = "rect_top"
        private const val KEY_R = "rect_right"
        private const val KEY_B = "rect_bottom"
    }
}