package com.savestatus.pro.model

enum class StatusType {
    IMAGE,
    VIDEO
}

data class StatusItem(
    val id: String,
    val filePath: String,
    val type: StatusType,
    val lastModified: Long,
    val duration: Long = 0L,
    val isDownloaded: Boolean = false
) {
    val fileName: String get() = filePath.substringAfterLast("/")
}

