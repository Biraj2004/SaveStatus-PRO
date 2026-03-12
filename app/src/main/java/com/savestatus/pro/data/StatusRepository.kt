package com.savestatus.pro.data

import android.content.Context
import com.savestatus.pro.model.StatusItem
import com.savestatus.pro.model.StatusType
import com.savestatus.pro.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class StatusRepository(private val context: Context) {

    suspend fun loadStatuses(isBusinessMode: Boolean): List<StatusItem> =
        withContext(Dispatchers.IO) {
            val dir = FileScanner.getStatusDirectory(isBusinessMode)
            FileScanner.scanStatuses(dir, context)
        }

    suspend fun downloadStatus(item: StatusItem): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val src = File(item.filePath)
                if (!src.exists()) return@withContext false
                val result = FileUtils.copyFileToSaveDir(src, item.type, context)
                result != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun getDownloadedStatuses(): List<StatusItem> =
        withContext(Dispatchers.IO) {
            val root = FileUtils.getSaveRootDirectory()
            val imageDir = File(root, FileUtils.IMAGES_DIR)
            val videoDir = File(root, FileUtils.VIDEOS_DIR)

            val items = mutableListOf<StatusItem>()

            if (imageDir.exists()) {
                imageDir.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
                    ?.forEach { file ->
                        items.add(
                            StatusItem(
                                id = file.absolutePath,
                                filePath = file.absolutePath,
                                type = StatusType.IMAGE,
                                lastModified = file.lastModified(),
                                isDownloaded = true
                            )
                        )
                    }
            }

            if (videoDir.exists()) {
                videoDir.listFiles()
                    ?.filter { it.isFile && it.extension.lowercase() in setOf("mp4", "mkv", "3gp") }
                    ?.forEach { file ->
                        val duration = FileScanner.getVideoDuration(file.absolutePath, context)
                        items.add(
                            StatusItem(
                                id = file.absolutePath,
                                filePath = file.absolutePath,
                                type = StatusType.VIDEO,
                                lastModified = file.lastModified(),
                                duration = duration,
                                isDownloaded = true
                            )
                        )
                    }
            }

            items.sortedByDescending { it.lastModified }
        }
}
