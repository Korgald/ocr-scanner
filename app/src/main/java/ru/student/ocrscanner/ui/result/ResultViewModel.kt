package ru.student.ocrscanner.ui.result

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.student.ocrscanner.recognition.RecognitionCache
import javax.inject.Inject

data class ResultUiState(
    val editedText: String = "",
    val confidence: Float = 0f
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val recognitionCache: RecognitionCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ResultUiState(
            editedText = recognitionCache.editedText ?: recognitionCache.result?.fullText ?: "",
            confidence = recognitionCache.result?.averageConfidence ?: 0f
        )
    )
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun updateText(newText: String) {
        _uiState.update { it.copy(editedText = newText) }
        recognitionCache.editedText = newText
    }

    fun prepareForSaving() {
        recognitionCache.editedText = _uiState.value.editedText
    }

    fun onBack() {
        recognitionCache.clear()
    }
}