package com.example.app.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.app.data.NasConfigurationManager
import com.example.app.data.NasFileManager
import com.example.app.Screen
import jcifs.smb.SmbFile
import kotlinx.coroutines.launch

@Composable
fun NasBrowserScreen(navController: NavController, snackbarHostState: SnackbarHostState, path: String = "") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val nasConfigurationManager = remember { NasConfigurationManager(context) }
    var files by remember { mutableStateOf<List<SmbFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val config = nasConfigurationManager.getConfiguration()
    val password = nasConfigurationManager.getPassword()

    if (config != null && password != null) {
        val nasFileManager = NasFileManager(config, password)
        LaunchedEffect(path) {
            isLoading = true
            nasFileManager.listFilesAndDirs(path)
                .onSuccess { listedFiles -> files = listedFiles }
                .onFailure { error ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Error listing NAS files: ${error.message}")
                    }
                }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (path.isNotEmpty()) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (config == null || password == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NAS not configured")
                Button(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Text("Go to Settings")
                }
            }
        } else {
            LazyColumn {
                items(files) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (file.isDirectory) {
                                    navController.navigate(Screen.Browser.withArgs(file.path))
                                }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Star else Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = file.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
