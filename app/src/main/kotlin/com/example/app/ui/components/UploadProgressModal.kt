package com.example.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun UploadProgressModal(
    file: File,
    progress: Float,
    bytesUploaded: Long,
    totalBytes: Long,
    uploadSpeed: Double,
    error: String?,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Do nothing */ },
        title = { Text(text = "Uploading ${file.name}") },
        text = {
            Column {
                if (error != null) {
                    Text(text = "Error: $error")
                } else {
                    LinearProgressIndicator(progress = progress)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "%.1f MB / %.1f MB".format(bytesUploaded / 1e6, totalBytes / 1e6))
                    Text(text = "Speed: %.2f MB/s".format(uploadSpeed / 1e6))
                }
            }
        },
        confirmButton = {
            if (error != null) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(if (error != null) "Close" else "Cancel")
            }
        }
    )
}
