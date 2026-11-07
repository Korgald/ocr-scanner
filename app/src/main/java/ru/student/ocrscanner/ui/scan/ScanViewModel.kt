package ru.student.ocrscanner.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.student.ocrscanner.camera.ImagePreprocessor
import ru.student.ocrscanner.recognition.RecognitionCache
import java.io.File
import javax.inject.Inject

data class ScanUiState(
    val hasCameraPermission: Boolean = false,
    val isDecoding: Boolean = false,
    val shouldNavigateToCrop: Boolean = false,
    val errorMessage: String? = null
)

// Decode photo/gallery image, persist to a fixed file (survives process death),
// and hand to crop screen via in-process cache for the fast path.
@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePreprocessor: ImagePreprocessor,
    private val recognitionCache: RecognitionCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun updateCameraPermission(granted: Boolean) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun onPhotoCaptured(file: File) = decode { imagePreprocessor.decodeOnly(file) }
    fun onPhotoPicked(uri: Uri) = decode { imagePreprocessor.decodeOnly(uri) }

    fun onNavigatedToCrop() { _uiState.update { it.copy(shouldNavigateToCrop = false) } }
    fun showError(message: String) { _uiState.update { it.copy(errorMessage = message) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    private fun decode(block: suspend () -> Bitmap?) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isDecoding = true, errorMessage = null) }
            val bitmap = try { block() } catch (e: Exception) { null }
            if (bitmap == null) {
                _uiState.update { it.copy(isDecoding = false, errorMessage = "Не удалось обработать изображение") }
                return@launch
            }
            recognitionCache.sourceBitmap = bitmap
            runCatching {
                File(context.filesDir, SRC_FILE).outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
            }
            _uiState.update { it.copy(isDecoding = false, shouldNavigateToCrop = true) }
        }
    }

    companion object { const val SRC_FILE = "crop_src.jpg" }
}