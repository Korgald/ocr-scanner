package ru.student.ocrscanner.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import ru.student.ocrscanner.camera.CameraPreview
import ru.student.ocrscanner.camera.CaptureController
import ru.student.ocrscanner.ui.scan.ScanViewModel
import ru.student.ocrscanner.ui.theme.OcrGradients
import ru.student.ocrscanner.ui.theme.Rose100

// Scan screen: camera preview with hint frame, gradient shutter, gallery picker.
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    onNavigateToCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ScanViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = remember { CaptureController() }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        viewModel.updateCameraPermission(cameraPermissionState.status.isGranted)
    }
    LaunchedEffect(uiState.shouldNavigateToCrop) {
        if (uiState.shouldNavigateToCrop) { viewModel.onNavigatedToCrop(); onNavigateToCrop() }
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri) else viewModel.showError("Фото не выбрано")
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.hasCameraPermission) {
            CameraPreview(
                controller = controller,
                onCaptured = viewModel::onPhotoCaptured,
                onError = viewModel::showError,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.88f)
                    .height(300.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Галерея",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp).size(28.dp))
                }

                // Gradient shutter: the visual centerpiece of the scan screen.
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(elevation = 8.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(OcrGradients.accent)
                        .clickable { controller.takePicture() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Сделать снимок",
                        tint = Rose100,
                        modifier = Modifier.size(36.dp))
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { }
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null,
                        tint = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(12.dp).size(28.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Для сканирования нужен доступ к камере.", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { cameraPermissionState.launchPermissionRequest() },
                    modifier = Modifier.padding(top = 16.dp)) { Text("Выдать разрешение") }
            }
        }

        uiState.errorMessage?.let { message ->
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::clearError) { Text("Закрыть") }
                }
            }
        }
        if (uiState.isDecoding) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}