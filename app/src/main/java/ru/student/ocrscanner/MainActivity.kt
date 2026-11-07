package ru.student.ocrscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.student.ocrscanner.ui.screens.MainScreen
import ru.student.ocrscanner.ui.screens.OnboardingScreen
import ru.student.ocrscanner.ui.settings.SettingsViewModel
import ru.student.ocrscanner.ui.theme.OcrScannerTheme

/** Entry point: no system action bar, title is drawn inside the UI. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kill the gray system header; the app name lives in MainScreen now.
        actionBar?.hide()
        setContent {
            val viewModel: SettingsViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            OcrScannerTheme(
                themeMode = settings.themeMode,
                fontScale = settings.fontScale
            ) {
                if (settings.onboardingCompleted) {
                    MainScreen()
                } else {
                    OnboardingScreen(onComplete = { viewModel.completeOnboarding() })
                }
            }
        }
    }
}