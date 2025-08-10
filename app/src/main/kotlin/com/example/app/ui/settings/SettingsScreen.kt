package com.example.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.app.data.NasConfigurationManager
import com.example.app.model.NasConfiguration
import com.example.app.ui.theme.InstaExporterTheme

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val nasConfigurationManager = remember { NasConfigurationManager(context) }

    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var sharedFolder by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val config = nasConfigurationManager.getConfiguration()
        if (config != null) {
            server = config.server
            username = config.username
            sharedFolder = config.sharedFolder
        }
        password = nasConfigurationManager.getPassword() ?: ""
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Server Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sharedFolder,
            onValueChange = { sharedFolder = it },
            label = { Text("Shared Folder") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val config = NasConfiguration(server, username, sharedFolder)
                nasConfigurationManager.saveConfiguration(config, password)
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    InstaExporterTheme {
        SettingsScreen()
    }
}
