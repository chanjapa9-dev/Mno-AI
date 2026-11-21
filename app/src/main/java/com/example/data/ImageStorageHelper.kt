package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ImageStorageHelper {

    suspend fun saveBitmapToAppFiles(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "mano_img"
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }

    suspend fun copyUriToAppFilesAndDecode(
        context: Context,
        uri: Uri
    ): Pair<String, Bitmap?> = withContext(Dispatchers.IO) {
        val bitmap = decodeScaledBitmapFromUri(context, uri, maxDim = 1280)
        if (bitmap == null) {
            return@withContext "" to null
        }
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val file = File(dir, "upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath to bitmap
    }

    suspend fun loadBitmapFromPath(path: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun decodeScaledBitmapFromUri(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val maxOrig = max(boundsOptions.outWidth, boundsOptions.outHeight)
        var sampleSize = 1
        while (maxOrig / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    suspend fun saveImageToGallery(context: Context, imagePath: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(imagePath)
            if (!file.exists()) return@withContext false

            val resolver = context.contentResolver
            val fileName = "ManoAI_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "Mano AI"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, values) ?: return@withContext false
            try {
                resolver.openOutputStream(itemUri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: return@withContext false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                }
                true
            } catch (_: Exception) {
                resolver.delete(itemUri, null, null)
                false
            }
        }

    fun buildShareImageIntent(context: Context, imagePath: String): Intent? {
        val file = File(imagePath)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
