package com.example.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.app.data.NasConfigurationManager
import com.example.app.data.NasFileManager
import com.example.app.model.UploadItem
import com.example.app.model.UploadStatus
import com.example.app.ui.browser.NasBrowserScreen
import com.example.app.ui.settings.SettingsScreen
import com.example.app.ui.theme.InstaExporterTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InstaExporterTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val items = listOf(
                    Screen.Main,
                    Screen.Settings
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.route) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Main.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Main.route) { MainScreen(navController, snackbarHostState) }
            composable(
                Screen.NasBrowser.route + "?path={path}",
                arguments = listOf(navArgument("path") { defaultValue = "" })
            ) { backStackEntry ->
                NasBrowserScreen(
                    navController,
                    snackbarHostState,
                    backStackEntry.arguments?.getString("path") ?: ""
                )
            }
            composable(Screen.Settings.route) { SettingsScreen(navController, snackbarHostState) }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector) {
    object Main : Screen("Main", Icons.Default.Home)
    object Settings : Screen("Settings", Icons.Default.Settings)
    object NasBrowser : Screen("NasBrowser", Icons.Default.Star)

    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("?path=$arg")
            }
        }
    }
}

@Composable
fun MainScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(false) }
    val nasConfigurationManager = remember { NasConfigurationManager(context) }
    var nasFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var uploadQueue by remember { mutableStateOf<List<UploadItem>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }

    val isNasSetup = nasConfigurationManager.getConfiguration() != null
    val isCameraConnected = getCameraFiles(context).isNotEmpty()

    val currentUploadingFile = uploadQueue.find { it.status == UploadStatus.UPLOADING }
    val window = (context as? Activity)?.window
    DisposableEffect(window, currentUploadingFile) {
        if (window != null && currentUploadingFile != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    LaunchedEffect(uploadQueue) {
        val nextToUpload = uploadQueue.find { it.status == UploadStatus.PENDING }
        if (nextToUpload != null && currentUploadingFile == null) {
            val config = nasConfigurationManager.getConfiguration()!!
            val password = nasConfigurationManager.getPassword()!!
            val nasFileManager = NasFileManager(config, password)

            val updateItem = { newItem: UploadItem ->
                uploadQueue = uploadQueue.map { if (it.file == newItem.file) newItem else it }
            }

            updateItem(nextToUpload.copy(status = UploadStatus.UPLOADING))

            nasFileManager.uploadFile(nextToUpload.file) { written, total ->
                val progress = if (total > 0) written.toFloat() / total.toFloat() else 0f
                updateItem(
                    nextToUpload.copy(
                        progress = progress,
                        bytesUploaded = written,
                        totalBytes = total,
                        status = UploadStatus.UPLOADING
                    )
                )
            }.onFailure {
                updateItem(nextToUpload.copy(status = UploadStatus.FAILED))
            }.onSuccess {
                nasFiles = nasFiles + nextToUpload.file.name
                updateItem(nextToUpload.copy(status = UploadStatus.UPLOADED))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasPermission = Environment.isExternalStorageManager()
        } else {
            hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    ChecklistItem(label = "NAS connection setup", isChecked = isNasSetup)
                    ChecklistItem(label = "Camera connected", isChecked = isCameraConnected)
                }
                IconButton(onClick = { refreshTrigger++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            if (hasPermission) {
                if (isNasSetup && isCameraConnected) {
                    val cameraFiles = getCameraFiles(context)
                    val config = nasConfigurationManager.getConfiguration()!!
                    val password = nasConfigurationManager.getPassword()!!
                    val nasFileManager = NasFileManager(config, password)

                    LaunchedEffect(refreshTrigger) {
                        isLoading = true
                        nasFileManager.listFiles()
                            .onSuccess { files -> nasFiles = files }
                            .onFailure { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Error listing NAS files: ${error.message}")
                                }
                            }
                        isLoading = false
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column {
                            FileList(
                                modifier = Modifier.weight(1f),
                                cameraFiles = cameraFiles,
                                nasFiles = nasFiles,
                                selectedFiles = selectedFiles,
                                onFileSelected = { file, isSelected ->
                                    selectedFiles = if (isSelected) {
                                        selectedFiles + file
                                    } else {
                                        selectedFiles - file
                                    }
                                },
                                uploadQueue = uploadQueue
                            )

                            if (selectedFiles.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val newItems = selectedFiles.map { UploadItem(it, UploadStatus.PENDING) }
                                        uploadQueue = uploadQueue + newItems
                                        selectedFiles = emptySet()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Text("Upload ${selectedFiles.size} selected files")
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }) {
                        Text("Request Permissions")
                    }
                }
            }
        }
        if (uploadQueue.isNotEmpty()) {
            UploadQueueUI(queue = uploadQueue)
        }
    }
}

@Composable
fun ChecklistItem(label: String, isChecked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isChecked) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun FileList(
    modifier: Modifier = Modifier,
    cameraFiles: List<File>,
    nasFiles: List<String>,
    selectedFiles: Set<File>,
    onFileSelected: (File, Boolean) -> Unit,
    uploadQueue: List<UploadItem>
) {
    LazyColumn(modifier = modifier) {
        items(cameraFiles) { file ->
            val isSelected = file in selectedFiles
            val isUploaded = nasFiles.contains(file.name)
            val inQueue = uploadQueue.any { it.file == file }
            val isEnabled = !isUploaded && !inQueue

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) { onFileSelected(file, !isSelected) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onFileSelected(file, it) },
                    enabled = isEnabled
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = file.name)
                    Text(text = formatFileSize(file.length()), style = MaterialTheme.typography.bodySmall)
                }

                if (isUploaded) {
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Uploaded",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else if (inQueue) {
                    val item = uploadQueue.find { it.file == file }
                    Spacer(Modifier.weight(1f))
                    when (item?.status) {
                        UploadStatus.PENDING -> Text("Pending...")
                        UploadStatus.UPLOADING -> {
                            val progress = (item.progress * 100).toInt()
                            Text("$progress%")
                        }
                        UploadStatus.UPLOADED -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Uploaded",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        UploadStatus.FAILED -> Text("Failed", color = MaterialTheme.colorScheme.error)
                        null -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun UploadQueueUI(queue: List<UploadItem>) {
    val uploading = queue.filter { it.status == UploadStatus.UPLOADING }
    val pending = queue.filter { it.status == UploadStatus.PENDING }
    val failed = queue.filter { it.status == UploadStatus.FAILED }

    val totalProgress = if (queue.isNotEmpty()) {
        val totalBytes = queue.sumOf { it.file.length() }
        val uploadedBytes = queue.sumOf { it.bytesUploaded }
        if (totalBytes > 0) uploadedBytes.toFloat() / totalBytes.toFloat() else 0f
    } else 0f

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Text("Upload Queue", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = totalProgress,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Overall: ${(totalProgress * 100).toInt()}% (${uploading.size} uploading, ${pending.size} pending, ${failed.size} failed)")
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun getCameraFiles(context: Context): List<File> {
    val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val storageVolumes = storageManager.storageVolumes

    val cameraVolume = storageVolumes.find { it.isRemovable } ?: return emptyList()
    val cameraDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        cameraVolume.directory
    } else {
        // This is a hack for older versions, might not work on all devices
        @Suppress("DEPRECATION")
        File("/storage/${cameraVolume.uuid}")
    }

    val dcimDir = File(cameraDir, "DCIM/Camera01")
    if (!dcimDir.exists()) {
        return emptyList()
    }

    return dcimDir.listFiles()?.toList() ?: emptyList()
}
