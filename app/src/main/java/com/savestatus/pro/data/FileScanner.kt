package com.savestatus.pro.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.util.Log
import com.savestatus.pro.model.StatusItem
import com.savestatus.pro.model.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FileScanner {

    private const val TAG = "FileScanner"

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "3gp")

    /**
     * Returns the best available status directory for WhatsApp or WhatsApp Business.
     * Tries multiple known paths in priority order and returns the first accessible one.
     */
    fun getStatusDirectory(isBusinessMode: Boolean): File {
        val sdcard = Environment.getExternalStorageDirectory()
        val candidates: List<File> = if (isBusinessMode) {
            listOf(
                // Android 11+ scoped storage path (primary)
                File(sdcard, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
                // Legacy path (Android < 11)
                File(sdcard, "WhatsApp Business/Media/.Statuses"),
                // Alternative legacy path
                File(sdcard, "Android/media/com.whatsapp.w4b/WhatsApp Business/.Statuses")
            )
        } else {
            listOf(
                // Android 11+ scoped storage path (primary)
                File(sdcard, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                // Legacy path (Android < 11)
                File(sdcard, "WhatsApp/Media/.Statuses"),
                // Alternative legacy path
                File(sdcard, "Android/media/com.whatsapp/WhatsApp/.Statuses")
            )
        }

        // On Android < 11, prefer legacy path first
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val legacyFirst = if (isBusinessMode) {
                listOf(
                    File(sdcard, "WhatsApp Business/Media/.Statuses"),
                    File(sdcard, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses")
                )
            } else {
                listOf(
                    File(sdcard, "WhatsApp/Media/.Statuses"),
                    File(sdcard, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
                )
            }
            legacyFirst.firstOrNull { it.exists() && it.canRead() }
                ?: legacyFirst.first()
        } else {
            candidates.firstOrNull { it.exists() && it.canRead() }
                ?: candidates.first()
        }
    }

    suspend fun scanStatuses(dir: File, context: Context): List<StatusItem> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Scanning directory: ${dir.absolutePath}")
            Log.d(TAG, "Exists: ${dir.exists()}, CanRead: ${dir.canRead()}")

            if (!dir.exists()) {
                Log.w(TAG, "Status directory does not exist: ${dir.absolutePath}")
                return@withContext emptyList()
            }
            if (!dir.canRead()) {
                Log.w(TAG, "Cannot read status directory: ${dir.absolutePath}")
                return@withContext emptyList()
            }

            val files = dir.listFiles()
            if (files == null) {
                Log.w(TAG, "listFiles() returned null for: ${dir.absolutePath}")
                return@withContext emptyList()
            }

            Log.d(TAG, "Found ${files.size} files in ${dir.absolutePath}")

            val allExtensions = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS
            val statusFiles = files.filter { file ->
                file.isFile && file.extension.lowercase() in allExtensions
            }

            Log.d(TAG, "Filtered to ${statusFiles.size} status files")

            statusFiles.map { file ->
                val ext = file.extension.lowercase()
                val type = if (ext in VIDEO_EXTENSIONS) StatusType.VIDEO else StatusType.IMAGE
                val duration = if (type == StatusType.VIDEO) {
                    getVideoDuration(file.absolutePath, context)
                } else 0L

                StatusItem(
                    id = file.absolutePath,
                    filePath = file.absolutePath,
                    type = type,
                    lastModified = file.lastModified(),
                    duration = duration,
                    isDownloaded = false
                )
            }.sortedByDescending { it.lastModified }
        }


    suspend fun getVideoDuration(filePath: String, @Suppress("UNUSED_PARAMETER") context: Context): Long =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(filePath)
                val durationStr = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )
                durationStr?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get video duration for: $filePath", e)
                0L
            } finally {
                try { retriever.release() } catch (_: Exception) { }
            }
        }
}

