package ru.student.ocrscanner.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Реализация экспорта: TXT пишется потоком, PDF рисуется через PdfDocument. */
@Singleton
class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ExportRepository {

    override suspend fun exportTxt(uri: Uri, title: String, text: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = if (title.isBlank()) text else "$title\n\n$text"
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                } ?: return@runCatching false
                true
            }.getOrElse { false }
        }

    override suspend fun exportPdf(uri: Uri, title: String, text: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                // Ширина/высота страницы в пикселях (A4 ~ 595x842 при 72 dpi).
                val pageWidthPx = 595
                val pageHeightPx = 842
                val marginPx = 40f

                val bodyPaint = TextPaint().apply {
                    textSize = 12f
                    color = Color.BLACK
                    isAntiAlias = true
                }

                // Доступная ширина текста (Int для StaticLayout).
                val contentWidth = (pageWidthPx - 2 * marginPx).toInt()
                val fullText = if (title.isBlank()) text else "$title\n\n$text"

                val fullLayout = createLayout(fullText, bodyPaint, contentWidth)
                if (fullLayout.lineCount == 0) return@runCatching false

                // getLineBottom/getLineTop возвращают int -> приводим к Float явно.
                val lineHeightPx: Int = if (fullLayout.lineCount > 1) {
                    fullLayout.getLineBottom(1) - fullLayout.getLineTop(1)
                } else {
                    fullLayout.getLineBottom(0) - fullLayout.getLineTop(0)
                }
                val safeLineHeight = lineHeightPx.toFloat().coerceAtLeast(1f)

                // Доступная высота страницы (Float) / высота строки (Float) = строк на страницу.
                val usableHeight = pageHeightPx - 2 * marginPx
                val linesPerPage = (usableHeight / safeLineHeight).toInt().coerceAtLeast(1)

                val document = PdfDocument()
                var pageNumber = 1
                var startLine = 0

                while (startLine < fullLayout.lineCount) {
                    val endLine = minOf(startLine + linesPerPage, fullLayout.lineCount)
                    val startOffset = fullLayout.getLineStart(startLine)
                    val endOffset = fullLayout.getLineEnd(endLine - 1).coerceAtMost(fullText.length)
                    val pageText = fullText.substring(startOffset, endOffset)

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        pageWidthPx, pageHeightPx, pageNumber
                    ).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    val pageLayout = createLayout(pageText, bodyPaint, contentWidth)
                    canvas.save()
                    canvas.translate(marginPx, marginPx)
                    pageLayout.draw(canvas)
                    canvas.restore()
                    document.finishPage(page)

                    startLine = endLine
                    pageNumber++
                }

                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    document.writeTo(out)
                } ?: run {
                    document.close()
                    return@runCatching false
                }
                document.close()
                true
            }.getOrElse { false }
        }

    /** Создаёт StaticLayout с переносом строк по ширине. */
    private fun createLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .build()
}