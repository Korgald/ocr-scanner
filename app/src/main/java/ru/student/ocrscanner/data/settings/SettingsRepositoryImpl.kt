package ru.student.ocrscanner.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Реализация настроек через DataStore Preferences. */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val OCR_LANGUAGE = stringPreferencesKey("ocr_language")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val settings: Flow<AppSettings> =
        dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { preferences ->
                AppSettings(
                    themeMode = preferences[Keys.THEME_MODE]
                        ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                        ?: ThemeMode.SYSTEM,
                    ocrLanguage = preferences[Keys.OCR_LANGUAGE]
                        ?.let { value -> OcrLanguage.entries.firstOrNull { it.name == value } }
                        ?: OcrLanguage.AUTO,
                    fontScale = (preferences[Keys.FONT_SCALE] ?: 1.0f)
                        .coerceIn(0.8f, 1.6f),
                    onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false
                )
            }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setOcrLanguage(language: OcrLanguage) {
        dataStore.edit { preferences ->
            preferences[Keys.OCR_LANGUAGE] = language.name
        }
    }

    override suspend fun setFontScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.FONT_SCALE] = scale.coerceIn(0.8f, 1.6f)
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
        }
    }
}
