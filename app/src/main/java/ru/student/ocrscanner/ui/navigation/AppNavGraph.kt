package ru.student.ocrscanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ru.student.ocrscanner.ui.notes.NoteEditorViewModel
import ru.student.ocrscanner.ui.screens.CropScreen
import ru.student.ocrscanner.ui.screens.NoteEditorScreen
import ru.student.ocrscanner.ui.screens.NotesScreen
import ru.student.ocrscanner.ui.screens.ResultScreen
import ru.student.ocrscanner.ui.screens.ScanScreen
import ru.student.ocrscanner.ui.screens.SettingsScreen

/** Navigation graph: scan -> crop -> result, plus notes/editor/settings. */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SCAN,
        modifier = modifier
    ) {
        composable(Routes.SCAN) {
            ScanScreen(
                onNavigateToCrop = {
                    navController.navigate(Routes.CROP) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.CROP) {
            CropScreen(
                onNavigateToResult = {
                    navController.navigate(Routes.RESULT) { launchSingleTop = true }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                onBack = { navController.popBackStack() },
                onSavedToNewNote = {
                    navController.navigate(Routes.noteEditorNew()) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.NOTES) {
            NotesScreen(
                onOpenEditor = { noteId ->
                    navController.navigate(Routes.noteEditor(noteId)) { launchSingleTop = true }
                },
                onCreateNew = {
                    navController.navigate(Routes.noteEditorNew()) { launchSingleTop = true }
                }
            )
        }
        composable(Routes.SETTINGS) { SettingsScreen() }
        composable(
            route = Routes.NOTE_EDITOR,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            val viewModel: NoteEditorViewModel = hiltViewModel()
            NoteEditorScreen(
                noteId = noteId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}