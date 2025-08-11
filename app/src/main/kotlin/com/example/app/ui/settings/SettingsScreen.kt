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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.app.data.NasConfigurationManager
import com.example.app.data.NasFileManager
import com.example.app.model.NasConfiguration
import com.example.app.ui.theme.InstaExporterTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val nasConfigurationManager = remember { NasConfigurationManager(context) }
    var isLoading by remember { mutableStateOf(false) }
    var isConfigurationValid by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }

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
        isConfigurationValid = nasConfigurationManager.getValidity()
        isExpanded = !isConfigurationValid
    }

    Column(modifier = Modifier.padding(16.dp)) {
        StatusHeader(isConfigurationValid) {
            isExpanded = !isExpanded
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
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
                        isLoading = true
                        val config = NasConfiguration(server, username, sharedFolder)
                        nasConfigurationManager.saveConfiguration(config, password)
                        val nasFileManager = NasFileManager(config, password)
                        scope.launch {
                            nasFileManager.testConnection()
                                .onSuccess {
                                    snackbarHostState.showSnackbar("Connection successful")
                                    isConfigurationValid = true
                                    nasConfigurationManager.saveValidity(true)
                                    isExpanded = false
                                }
                                .onFailure { error ->
                                    snackbarHostState.showSnackbar("Connection failed: ${error.message}")
                                    isConfigurationValid = false
                                    nasConfigurationManager.saveValidity(false)
                                }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Test Connection")
                }
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun StatusHeader(isValid: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isValid) Color.Green.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f)
    val text = if (isValid) "Configuration is valid and saved" else "No valid configuration"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = text)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand/Collapse")
        }
    }
}
