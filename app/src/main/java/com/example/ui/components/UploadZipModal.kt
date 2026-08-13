package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.util.ZipUtils

@Composable
fun UploadZipModal(
    uploadStatus: String?,
    uploadProgress: Int,
    onDismiss: () -> Unit,
    onUploadZip: (Uri, String, String?) -> Unit,
    onOpenSamplePicker: () -> Unit
) {
    val context = LocalContext.current
    var selectedZipUri by remember { mutableStateOf<Uri?>(null) }
    var appNameInput by remember { mutableStateOf("") }
    var repoNameInput by remember { mutableStateOf("") }
    var zipFileName by remember { mutableStateOf<String?>(null) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedZipUri = uri
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "app_code.zip"
            zipFileName = fileName
            if (appNameInput.isBlank()) {
                appNameInput = fileName.removeSuffix(".zip").replace("_", " ").replace("-", " ")
                    .split(" ")
                    .joinToString(" ") { it.capitalize() }
            }
        }
    }

    Dialog(onDismissRequest = { if (uploadStatus == null) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("upload_zip_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Compile App Code (.zip)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = uploadStatus == null
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                if (uploadStatus != null) {
                    // Upload / Dispatch Progress view
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = uploadStatus,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        LinearProgressIndicator(
                            progress = { (uploadProgress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // File Selection & Form Input
                    OutlinedButton(
                        onClick = { zipPickerLauncher.launch("application/zip") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderZip, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (zipFileName != null) "Selected: $zipFileName" else "Select .zip File from Storage")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "OR", style = MaterialTheme.typography.labelSmall)
                    }

                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenSamplePicker()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Pre-built Sample Android Project")
                    }

                    OutlinedTextField(
                        value = appNameInput,
                        onValueChange = { appNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("App Name") },
                        placeholder = { Text("My Awesome App") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = repoNameInput,
                        onValueChange = { repoNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target Repository Name (Optional)") },
                        placeholder = { Text("my-awesome-app") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "The app automatically injects .github/workflows/android-build.yml to compile your code on GitHub Actions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val uri = selectedZipUri
                                if (uri != null && appNameInput.isNotBlank()) {
                                    onUploadZip(uri, appNameInput, repoNameInput)
                                }
                            },
                            enabled = selectedZipUri != null && appNameInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("submit_upload_zip_button")
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload & Compile")
                        }
                    }
                }
            }
        }
    }
}
