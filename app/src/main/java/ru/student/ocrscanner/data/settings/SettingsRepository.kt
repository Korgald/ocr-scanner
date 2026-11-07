package ru.student.ocrscanner.data.settings

import kotlinx.coroutines.flow.Flow

/** Репозиторий настроек. */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setOcrLanguage(language: OcrLanguage)
    suspend fun setFontScale(scale: Float)
    suspend fun setOnboardingCompleted(completed: Boolean)
}
