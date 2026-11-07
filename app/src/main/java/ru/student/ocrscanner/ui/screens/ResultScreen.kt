package ru.student.ocrscanner.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.student.ocrscanner.ui.result.ResultViewModel
import ru.student.ocrscanner.ui.theme.OcrGradients
import ru.student.ocrscanner.ui.theme.Rose100

// Result screen: gradient confidence hero + editable full text + actions.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onSavedToNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ResultViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Результат распознавания") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBack(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gradient hero card with big confidence number.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(OcrGradients.accent)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Уверенность распознавания",
                        style = MaterialTheme.typography.labelMedium,
                        color = Rose100.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${(uiState.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Rose100
                    )
                }
            }

            OutlinedTextField(
                value = uiState.editedText,
                onValueChange = viewModel::updateText,
                label = { Text("Распознанный текст") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 10
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { clipboardManager.setText(AnnotatedString(uiState.editedText)) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Копировать")
                }
                IconButton(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, uiState.editedText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }) { Icon(Icons.Filled.Share, contentDescription = "Поделиться") }
                IconButton(onClick = { viewModel.prepareForSaving(); onSavedToNewNote() }) {
                    Icon(Icons.Filled.Save, contentDescription = "Сохранить в заметки")
                }
                IconButton(onClick = { viewModel.onBack(); onBack() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Переснять")
                }
            }
        }
    }
}