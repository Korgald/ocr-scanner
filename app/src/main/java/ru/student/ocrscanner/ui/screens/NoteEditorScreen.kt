package ru.student.ocrscanner.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.student.ocrscanner.ui.notes.NoteEditorViewModel

// Editor with EXPLICIT save buttons and autosave on any exit (no lost edits).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportMessage by viewModel.exportMessage.collectAsStateWithLifecycle()
    val saveMessage by viewModel.saveMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val createTxt = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> if (uri != null) viewModel.exportTxt(uri) }
    val createPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> if (uri != null) viewModel.exportPdf(uri) }

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    LaunchedEffect(uiState.savedEvent) {
        if (uiState.savedEvent) {
            viewModel.consumeSavedEvent()
            onBack()
        }
    }

    LaunchedEffect(exportMessage) {
        exportMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeExportMessage()
        }
    }

    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeSaveMessage()
        }
    }

    // System back also saves, so edits are never lost silently.
    BackHandler { viewModel.saveAndExit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "Новая заметка" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.saveAndExit() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveAndStay() }) {
                        Icon(Icons.Filled.Save, contentDescription = "Сохранить")
                    }
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, uiState.note.text)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }) { Icon(Icons.Filled.Share, contentDescription = "Поделиться") }
                    IconButton(onClick = {
                        val name = (uiState.note.title.ifBlank { "заметка" }) + ".txt"
                        createTxt.launch(name)
                    }) { Icon(Icons.Filled.TextSnippet, contentDescription = "Экспорт TXT") }
                    IconButton(onClick = {
                        val name = (uiState.note.title.ifBlank { "заметка" }) + ".pdf"
                        createPdf.launch(name)
                    }) { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Экспорт PDF") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.saveAndExit() }) {
                Icon(Icons.Filled.Save, contentDescription = "Сохранить и выйти")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.note.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.note.text,
                onValueChange = viewModel::updateText,
                label = { Text("Текст") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 10
            )

            // Explicit, unmissable save actions at the bottom of the editor.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.saveAndStay() },
                    modifier = Modifier.weight(1f)
                ) { Text("Сохранить") }
                OutlinedButton(
                    onClick = { viewModel.saveAndExit() },
                    modifier = Modifier.weight(1f)
                ) { Text("Сохранить и выйти") }
            }
        }
    }
}