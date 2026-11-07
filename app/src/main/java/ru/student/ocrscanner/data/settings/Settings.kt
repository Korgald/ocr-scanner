package ru.student.ocrscanner.data.settings

/** Режим темы приложения. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** Язык распознавания OCR. */
enum class OcrLanguage {
    AUTO,
    RUSSIAN,
    LATIN
}

/** Пользовательские настройки приложения. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val ocrLanguage: OcrLanguage = OcrLanguage.AUTO,
    val fontScale: Float = 1.0f,
    val onboardingCompleted: Boolean = false
)
