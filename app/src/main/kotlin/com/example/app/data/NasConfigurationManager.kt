package com.example.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.app.model.NasConfiguration

class NasConfigurationManager(context: Context) {

    private val prefs: SharedPreferences
    private val encryptedPrefs: SharedPreferences

    init {
        // Regular SharedPreferences for non-sensitive data
        prefs = context.getSharedPreferences("nas_prefs", Context.MODE_PRIVATE)

        // EncryptedSharedPreferences for sensitive data
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "nas_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveConfiguration(nasConfiguration: NasConfiguration, password: String) {
        prefs.edit()
            .putString("server", nasConfiguration.server)
            .putString("username", nasConfiguration.username)
            .putString("sharedFolder", nasConfiguration.sharedFolder)
            .apply()

        encryptedPrefs.edit()
            .putString("password", password)
            .apply()
    }

    fun getConfiguration(): NasConfiguration? {
        val server = prefs.getString("server", null)
        val username = prefs.getString("username", null)
        val sharedFolder = prefs.getString("sharedFolder", null)

        if (server == null || username == null || sharedFolder == null) {
            return null
        }

        return NasConfiguration(server, username, sharedFolder)
    }

    fun getPassword(): String? {
        return encryptedPrefs.getString("password", null)
    }
}
