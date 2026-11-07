package ru.student.ocrscanner.ui.navigation

/** Application routes. */
object Routes {
    const val SCAN = "scan"
    const val CROP = "crop"
    const val RESULT = "result"
    const val NOTES = "notes"
    const val SETTINGS = "settings"
    const val NOTE_EDITOR = "note_editor/{noteId}"

    fun noteEditorNew(): String = "note_editor/-1"
    fun noteEditor(noteId: Long): String = "note_editor/$noteId"
}