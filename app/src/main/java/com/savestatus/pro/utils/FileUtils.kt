package com.savestatus.pro.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.savestatus.pro.model.StatusType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

object FileUtils {

    private const val TAG = "FileUtils"

    // Folder name in root storage: /storage/emulated/0/SaveStatus PRO/
    const val SAVE_DIR_NAME = "SaveStatus PRO"
    const val IMAGES_DIR = "Images"
    const val VIDEOS_DIR = "Videos"

    /**
     * Returns /storage/emulated/0/SaveStatus PRO/Images  or  .../Videos
     * Creates the directory if it doesn't exist.
     */
    fun getSaveDirectory(type: StatusType): File {
        val root = Environment.getExternalStorageDirectory()
        val subDir = if (type == StatusType.IMAGE) IMAGES_DIR else VIDEOS_DIR
        val dir = File(root, "$SAVE_DIR_NAME/$subDir")
        if (!dir.exists()) {
            val created = dir.mkdirs()
            Log.d(TAG, "Created dir ${dir.absolutePath}: $created")
        }
        return dir
    }

    /**
     * Returns /storage/emulated/0/SaveStatus PRO/
     * Creates the directory if it doesn't exist.
     */
    fun getSaveRootDirectory(): File {
        val root = Environment.getExternalStorageDirectory()
        val dir = File(root, SAVE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Copies a WhatsApp status file into the app's save folder and notifies the MediaStore.
     * Returns the destination file on success, null on failure.
     */
    fun copyFileToSaveDir(src: File, type: StatusType, context: Context): File? {
        return try {
            val destDir = getSaveDirectory(type)
            val destFile = File(destDir, src.name)
            if (destFile.exists()) {
                Log.d(TAG, "File already exists: ${destFile.absolutePath}")
                return destFile
            }

            FileInputStream(src).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Copied to: ${destFile.absolutePath}")
            notifyMediaStore(context, destFile, type)
            destFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy file: ${src.absolutePath}", e)
            null
        }
    }

    /**
     * Notifies MediaStore so the file appears in the Gallery immediately.
     */
    fun notifyMediaStore(context: Context, file: File, type: StatusType = StatusType.IMAGE) {
        val isVideo = type == StatusType.VIDEO || file.extension.lowercase() == "mp4"
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: insert into MediaStore properly
            val relativePath = if (isVideo) {
                "Movies/$SAVE_DIR_NAME"
            } else {
                "Pictures/$SAVE_DIR_NAME"
            }
            val contentUri = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            try {
                context.contentResolver.insert(contentUri, values)
            } catch (e: Exception) {
                Log.e(TAG, "MediaStore insert failed", e)
            }
        } else {
            // Android < 10: use MediaScanner
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
                null
            )
        }
    }

    fun isAlreadyDownloaded(src: File): Boolean {
        val type = if (src.extension.lowercase() in setOf("mp4", "mkv", "3gp"))
            StatusType.VIDEO else StatusType.IMAGE
        val destDir = getSaveDirectory(type)
        return File(destDir, src.name).exists()
    }

    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0:00"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    fun getFileUri(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
    }
}
