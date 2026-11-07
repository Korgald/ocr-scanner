package ru.student.ocrscanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.student.ocrscanner.data.settings.AppSettings
import ru.student.ocrscanner.data.settings.OcrLanguage
import ru.student.ocrscanner.data.settings.SettingsRepository
import ru.student.ocrscanner.data.settings.ThemeMode
import javax.inject.Inject

/** ViewModel настроек: чтение и изменение пользовательских настроек. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings()
        )

    /** Отметить онбординг пройденным. */
    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setOcrLanguage(language: OcrLanguage) {
        viewModelScope.launch { settingsRepository.setOcrLanguage(language) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setFontScale(scale) }
    }
}
