package com.example.app.model

import java.io.File

data class UploadItem(
    val file: File,
    val status: UploadStatus,
    val progress: Float = 0f,
    val totalBytes: Long = 0L,
    val bytesUploaded: Long = 0L
)

enum class UploadStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED
}
