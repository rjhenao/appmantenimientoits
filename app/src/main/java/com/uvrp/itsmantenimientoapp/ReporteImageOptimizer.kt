package com.uvrp.itsmantenimientoapp

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reduce peso de fotos antes de subirlas al servidor (reporte QR).
 * Raster → JPEG ~1920 px lado mayor, calidad ~82 %. PDF y demás no se tocan.
 */
object ReporteImageOptimizer {

    private const val TAG = "ReporteImageOpt"
    private const val MAX_LONG_EDGE_PX = 1920
    private const val JPEG_QUALITY = 82

    fun isRasterImageMime(mime: String?): Boolean {
        return when (mime?.lowercase()?.substringBefore(';')?.trim()) {
            "image/jpeg", "image/jpg", "image/png", "image/webp",
            "image/heic", "image/heif",
            -> true
            else -> false
        }
    }

    /**
     * @return bytes JPEG + nombre sugerido con extensión .jpg, o null si conviene usar el archivo original.
     */
    fun optimizeRasterToJpeg(
        resolver: ContentResolver,
        uri: Uri,
        suggestedBaseName: String,
    ): Pair<ByteArray, String>? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.w(TAG, "Bounds inválidos para $uri")
                return null
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, MAX_LONG_EDGE_PX)
            }

            var bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            try {
                bitmap = applyExifRotation(resolver, uri, bitmap)
                bitmap = scaleLongEdgeDown(bitmap, MAX_LONG_EDGE_PX)

                val os = ByteArrayOutputStream()
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, os)) {
                    Log.w(TAG, "compress JPEG falló")
                    return null
                }
                val bytes = os.toByteArray()
                val name = suggestedBaseName
                    .substringBeforeLast('.')
                    .ifBlank { "foto" }
                    .take(80) + ".jpg"

                Log.d(
                    TAG,
                    "Optimizado: ${bounds.outWidth}x${bounds.outHeight} → ${bitmap.width}x${bitmap.height}, ${bytes.size / 1024} KB JPEG",
                )
                return Pair(bytes, name)
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo optimizar imagen, se usará original: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(bounds: BitmapFactory.Options, maxSide: Int): Int {
        var inSampleSize = 1
        var h = bounds.outHeight
        var w = bounds.outWidth
        while (h > maxSide || w > maxSide) {
            inSampleSize *= 2
            h /= 2
            w /= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun applyExifRotation(resolver: ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = resolver.openInputStream(uri)?.use { input ->
            try {
                val exif = ExifInterface(input)
                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } catch (_: Exception) {
                0f
            }
        } ?: 0f

        if (degrees == 0f) return bitmap
        val m = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleLongEdgeDown(bitmap: Bitmap, maxLongEdge: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longEdge = max(w, h)
        if (longEdge <= maxLongEdge) return bitmap

        val scale = maxLongEdge.toFloat() / longEdge
        val nw = (w * scale).roundToInt().coerceAtLeast(1)
        val nh = (h * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }
}
