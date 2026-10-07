package com.hpg.goaltracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Copies user-picked images into app-private storage and loads them back.
 * Keeping a local copy avoids content-URI permission issues across restarts.
 */
object ImageStore {

    private const val MAX_DIMEN = 1024

    fun saveFromUri(context: Context, uri: Uri): String? = runCatching {
        val resolver = context.contentResolver

        // First pass: read bounds to compute a sample size and avoid OOM.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = computeSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMEN)

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val dst = File(dir, "img_${System.currentTimeMillis()}.jpg")
        FileOutputStream(dst).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
        dst.absolutePath
    }.getOrNull()

    fun loadBitmap(path: String): Bitmap? =
        runCatching { BitmapFactory.decodeFile(path) }.getOrNull()

    private fun computeSampleSize(width: Int, height: Int, maxDimen: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDimen && h / 2 >= maxDimen) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }
}
