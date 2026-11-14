package com.frostlock.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.frostlock.app.ui.util.SessionPhotos
import java.io.File
import java.io.FileOutputStream

/**
 * Copies picked photos into app-private storage and resolves them back for the
 * summary and diary. Uses the system photo picker, so FrostLock never needs the
 * camera or storage permission.
 */
object SessionPhotoStore {

    /** Copies [uri] for [sessionId], downsampling and rotating as needed. */
    fun save(context: Context, sessionId: Long, uri: Uri): String? = try {
        saveInternal(context, sessionId, uri)
    } catch (t: Throwable) {
        Log.w("SessionPhotoStore", "Could not copy session photo", t)
        null
    }

    private fun saveInternal(context: Context, sessionId: Long, uri: Uri): String? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val opened = resolver.openInputStream(uri) ?: return null
        opened.use { input ->
            // Bounds-only decodes return null by design; read the options instead.
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = SessionPhotos.sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val oriented = applyRotation(context, uri, decoded)
        if (oriented !== decoded) decoded.recycle()

        val name = SessionPhotos.fileName(sessionId)
        val directory = directory(context)
        if (!directory.exists() && !directory.mkdirs()) {
            oriented.recycle()
            return null
        }
        FileOutputStream(File(directory, name)).use { output ->
            oriented.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        oriented.recycle()
        return name
    }

    fun delete(context: Context, name: String) {
        if (!SessionPhotos.isManagedName(name)) return
        runCatching { File(directory(context), name).delete() }
    }

    fun file(context: Context, name: String): File? {
        if (!SessionPhotos.isManagedName(name)) return null
        return File(directory(context), name).takeIf { it.isFile }
    }

    private fun directory(context: Context): File =
        File(context.filesDir, SessionPhotos.DIRECTORY)

    private fun applyRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                when (
                    ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
