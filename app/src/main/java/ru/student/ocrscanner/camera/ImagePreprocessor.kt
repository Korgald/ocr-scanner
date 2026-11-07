package ru.student.ocrscanner.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preprocessor for OCR input images (pure Android graphics, no OpenCV).
 * Smart path: if the source is already high-contrast (screenshot, scan),
 * we skip median denoise and adaptive binarization, because those steps
 * erode thin glyph strokes and produce scattered unrecognized symbols.
 */
@Singleton
class ImagePreprocessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val maxLongSide = 2600
    private val minLongSide = 1200
    private val blockRadius = 12
    private val thresholdOffset = 12f

    suspend fun decodeOnly(file: File): Bitmap? =
        runCatching { decodeAndRotate(file.absolutePath) }.getOrNull()

    suspend fun decodeOnly(uri: Uri): Bitmap? = runCatching {
        val tmp = File(context.cacheDir, "picked_" + System.currentTimeMillis() + ".jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { output -> input.copyTo(output) }
        } ?: return null
        decodeAndRotate(tmp.absolutePath)
    }.getOrNull()

    /** Scale, then choose clean-grayscale path or full binarize path. */
    suspend fun prepareForOcr(src: Bitmap): Bitmap? = runCatching {
        val scaled = scaleToTarget(src)
        if (isAlreadyClean(scaled)) toGrayscale(scaled) else binarize(scaled)
    }.getOrElse { null }

    suspend fun process(file: File): Bitmap? = runCatching {
        val rotated = decodeAndRotate(file.absolutePath) ?: return null
        prepareForOcrInternal(rotated)
    }.getOrElse { null }

    suspend fun process(uri: Uri): Bitmap? = runCatching {
        val tmp = File(context.cacheDir, "picked_" + System.currentTimeMillis() + ".jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { output -> input.copyTo(output) }
        } ?: return null
        val rotated = decodeAndRotate(tmp.absolutePath) ?: return null
        prepareForOcrInternal(rotated)
    }.getOrElse { null }

    private fun prepareForOcrInternal(src: Bitmap): Bitmap {
        val scaled = scaleToTarget(src)
        return if (isAlreadyClean(scaled)) toGrayscale(scaled) else binarize(scaled)
    }

    /** Sample luminance; if most pixels are near-black or near-white, image is clean. */
    private fun isAlreadyClean(src: Bitmap): Boolean {
        val w = src.width
        val h = src.height
        val argb = IntArray(w * h)
        src.getPixels(argb, 0, w, 0, 0, w, h)
        val step = maxOf(1, (w * h) / 20000)
        var hit = 0
        var total = 0
        var i = 0
        while (i < argb.size) {
            val p = argb[i]
            val lum = (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)).toInt()
            if (lum < 64 || lum > 192) hit++
            total++
            i += step
        }
        return total > 0 && (hit.toFloat() / total) > 0.85f
    }

    /** Grayscale without denoise/binarize: preserves thin glyph strokes. */
    private fun toGrayscale(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val argb = IntArray(w * h)
        src.getPixels(argb, 0, w, 0, 0, w, h)
        for (i in argb.indices) {
            val p = argb[i]
            val g = (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)).toInt()
            argb[i] = Color.rgb(g, g, g)
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(argb, 0, w, 0, 0, w, h)
        if (out !== src) src.recycle()
        return out
    }

    private fun decodeAndRotate(path: String): Bitmap? = try {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        if (degrees != 0f) {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: OutOfMemoryError) {
        null
    }

    private fun scaleToTarget(src: Bitmap): Bitmap {
        val longSide = maxOf(src.width, src.height)
        val target = when {
            longSide > maxLongSide -> maxLongSide
            longSide < minLongSide -> minLongSide
            else -> return src
        }
        val scale = target.toFloat() / longSide
        val newW = (src.width * scale).toInt().coerceAtLeast(1)
        val newH = (src.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        if (scaled !== src) src.recycle()
        return scaled
    }

    /** Full path for photos with uneven light: denoise + adaptive binarize. */
    private fun binarize(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val argb = IntArray(w * h)
        src.getPixels(argb, 0, w, 0, 0, w, h)

        val gray = IntArray(w * h)
        for (i in 0 until gray.size) {
            val p = argb[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            gray[i] = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }

        medianFilter3x3(gray, w, h)

        val stride = w + 1
        val integral = IntArray(stride * (h + 1))
        for (y in 0 until h) {
            var rowSum = 0
            for (x in 0 until w) {
                rowSum += gray[y * w + x]
                integral[(y + 1) * stride + (x + 1)] =
                    integral[y * stride + (x + 1)] + rowSum
            }
        }

        val rad = blockRadius
        for (y in 0 until h) {
            val y0 = (y - rad).coerceAtLeast(0)
            val y1 = (y + rad).coerceAtMost(h - 1)
            for (x in 0 until w) {
                val x0 = (x - rad).coerceAtLeast(0)
                val x1 = (x + rad).coerceAtMost(w - 1)
                val sum = integral[(y1 + 1) * stride + (x1 + 1)] -
                    integral[y0 * stride + (x1 + 1)] -
                    integral[(y1 + 1) * stride + x0] +
                    integral[y0 * stride + x0]
                val count = (x1 - x0 + 1) * (y1 - y0 + 1)
                val mean = sum.toFloat() / count
                val idx = y * w + x
                gray[idx] = if (gray[idx] < mean - thresholdOffset) 0 else 255
            }
        }

        for (i in 0 until gray.size) {
            gray[i] = if (gray[i] == 0) Color.BLACK else Color.WHITE
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(gray, 0, w, 0, 0, w, h)
        if (out !== src) src.recycle()
        return out
    }

    private fun medianFilter3x3(gray: IntArray, w: Int, h: Int) {
        val out = gray.copyOf()
        val buf = IntArray(9)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var k = 0
                for (dy in -1..1) {
                    val yy = (y + dy).coerceIn(0, h - 1)
                    for (dx in -1..1) {
                        val xx = (x + dx).coerceIn(0, w - 1)
                        buf[k] = gray[yy * w + xx]
                        k++
                    }
                }
                Arrays.sort(buf)
                out[y * w + x] = buf[4]
            }
        }
        System.arraycopy(out, 0, gray, 0, gray.size)
    }
}