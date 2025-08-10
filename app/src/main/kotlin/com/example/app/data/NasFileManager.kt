package com.example.app.data

import com.example.app.model.NasConfiguration
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NasFileManager(
    private val nasConfiguration: NasConfiguration,
    private val password: String
) {
    suspend fun listFiles(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val baseContext = SingletonContext.getInstance()
            val auth = NtlmPasswordAuthentication(baseContext, null, nasConfiguration.username, password)
            val contextWithCreds = baseContext.withCredentials(auth)
            val url = "smb://${nasConfiguration.server}/${nasConfiguration.sharedFolder}/"
            val dir = SmbFile(url, contextWithCreds)
            Result.success(dir.listFiles()?.map { it.name } ?: emptyList())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseContext = SingletonContext.getInstance()
            val auth = NtlmPasswordAuthentication(baseContext, null, nasConfiguration.username, password)
            val contextWithCreds = baseContext.withCredentials(auth)
            val url = "smb://${nasConfiguration.server}/${nasConfiguration.sharedFolder}/"
            val dir = SmbFile(url, contextWithCreds)
            dir.listFiles() // Just try to list files
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun listFilesAndDirs(path: String): Result<List<SmbFile>> = withContext(Dispatchers.IO) {
        try {
            val baseContext = SingletonContext.getInstance()
            val auth = NtlmPasswordAuthentication(baseContext, null, nasConfiguration.username, password)
            val contextWithCreds = baseContext.withCredentials(auth)
            val url = "smb://${nasConfiguration.server}/${nasConfiguration.sharedFolder}/$path"
            val dir = SmbFile(url, contextWithCreds)
            Result.success(dir.listFiles()?.toList() ?: emptyList())
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
