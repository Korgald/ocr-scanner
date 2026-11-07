package ru.student.ocrscanner.camera

/**
 * Простой контроллер съёмки: превью устанавливает действие,
 * а экран вызывает takePicture() из кнопки.
 */
class CaptureController {

    internal var captureAction: (() -> Unit)? = null

    /** Готов ли камера к съёмке. */
    val isReady: Boolean
        get() = captureAction != null

    /** Сделать снимок, если камера готова. */
    fun takePicture() {
        captureAction?.invoke()
    }

    /** Освободить действие при уничтожении превью. */
    internal fun release() {
        captureAction = null
    }
}
