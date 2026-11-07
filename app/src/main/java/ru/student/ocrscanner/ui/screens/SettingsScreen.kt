package ru.student.ocrscanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.student.ocrscanner.data.settings.OcrLanguage
import ru.student.ocrscanner.data.settings.ThemeMode
import ru.student.ocrscanner.ui.settings.SettingsViewModel

/** Экран настроек: тема, размер текста, язык OCR, о приложении. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки") }) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Тема приложения.
            Text(text = "Тема", style = MaterialTheme.typography.titleMedium)
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) }
                    )
                    Text(
                        text = themeLabel(mode),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Размер текста.
            Text(
                text = "Размер текста: %.1fx".format(settings.fontScale),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.fontScale,
                onValueChange = viewModel::setFontScale,
                valueRange = 0.8f..1.6f,
                steps = 7
            )

            // Язык OCR.
            Text(text = "Язык распознавания", style = MaterialTheme.typography.titleMedium)
            OcrLanguage.entries.forEach { language ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.ocrLanguage == language,
                        onClick = { viewModel.setOcrLanguage(language) }
                    )
                    Text(
                        text = languageLabel(language),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // О приложении.
            Text(text = "О приложении", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "OCR Сканер шпаргалок и конспектов, версия 1.0.\n" +
                    "Распознавание офлайн: Tesseract4Android (кириллица + латиница).\n" +
                    "Учебный проект.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Системная"
    ThemeMode.LIGHT -> "Светлая"
    ThemeMode.DARK -> "Тёмная"
}

private fun languageLabel(language: OcrLanguage): String = when (language) {
    OcrLanguage.AUTO -> "Авто (рус + лат)"
    OcrLanguage.RUSSIAN -> "Русский"
    OcrLanguage.LATIN -> "Латиница"
}
