package ru.student.ocrscanner.ui.notes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.student.ocrscanner.data.Note
import ru.student.ocrscanner.data.NoteRepository
import ru.student.ocrscanner.data.export.ExportRepository
import ru.student.ocrscanner.recognition.RecognitionCache
import javax.inject.Inject

data class NoteEditorUiState(
    val note: Note = Note(title = "", text = ""),
    val isLoaded: Boolean = false,
    val isNew: Boolean = true,
    val savedEvent: Boolean = false
)

// Editor VM with two explicit save modes and a guard against saving before
// the note finished loading (this was the "saves every other time" bug).
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val exportRepository: ExportRepository,
    private val recognitionCache: RecognitionCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    fun load(noteId: Long, initialText: String? = null) {
        if (_uiState.value.isLoaded) return
        viewModelScope.launch {
            if (noteId > 0) {
                val existing = noteRepository.getNote(noteId)
                if (existing != null) {
                    _uiState.update { it.copy(note = existing, isLoaded = true, isNew = false) }
                } else {
                    val seed = initialText ?: recognitionCache.editedText ?: ""
                    _uiState.update { it.copy(note = Note(title = "", text = seed), isLoaded = true, isNew = true) }
                }
            } else {
                val seed = initialText ?: recognitionCache.editedText ?: ""
                recognitionCache.editedText = null
                _uiState.update { it.copy(note = Note(title = "", text = seed), isLoaded = true, isNew = true) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(note = it.note.copy(title = title)) }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(note = it.note.copy(text = text)) }
    }

    // Save and stay on screen, show toast.
    fun saveAndStay() {
        viewModelScope.launch {
            if (doSave()) _saveMessage.value = "Сохранено"
        }
    }

    // Save and navigate back (used by FAB, back arrow and system back).
    fun saveAndExit() {
        viewModelScope.launch {
            if (doSave()) _uiState.update { it.copy(savedEvent = true) }
        }
    }

    fun consumeSavedEvent() { _uiState.update { it.copy(savedEvent = false) } }
    fun consumeSaveMessage() { _saveMessage.value = null }

    private suspend fun doSave(): Boolean {
        if (!_uiState.value.isLoaded) {
            _saveMessage.value = "Заметка ещё загружается, подожди секунду"
            return false
        }
        val current = _uiState.value.note
        val updated = current.copy(updatedAt = System.currentTimeMillis())
        val newId = noteRepository.saveNote(updated)
        _uiState.update { it.copy(note = it.note.copy(id = newId)) }
        return true
    }

    fun exportTxt(uri: Uri) {
        viewModelScope.launch {
            val note = _uiState.value.note
            val ok = exportRepository.exportTxt(uri, note.title, note.text)
            _exportMessage.value = if (ok) "Файл TXT сохранён" else "Не удалось экспортировать TXT"
        }
    }

    fun exportPdf(uri: Uri) {
        viewModelScope.launch {
            val note = _uiState.value.note
            val ok = exportRepository.exportPdf(uri, note.title, note.text)
            _exportMessage.value = if (ok) "Файл PDF сохранён" else "Не удалось экспортировать PDF"
        }
    }

    fun consumeExportMessage() { _exportMessage.value = null }
}