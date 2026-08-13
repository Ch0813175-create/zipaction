package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class SampleTemplateItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun SampleProjectPickerModal(
    onDismiss: () -> Unit,
    onSelectSample: (templateType: String, appName: String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf("compose_counter") }
    var appNameInput by remember { mutableStateOf("Jetpack Counter App") }

    val templates = listOf(
        SampleTemplateItem(
            id = "compose_counter",
            title = "Jetpack Compose Counter App",
            description = "Complete Android Jetpack Compose app with Material 3 state counter.",
            icon = Icons.Default.AddCircleOutline
        ),
        SampleTemplateItem(
            id = "todo_app",
            title = "Task Master Todo App",
            description = "Android Jetpack Compose Todo manager with list layouts.",
            icon = Icons.Default.Checklist
        ),
        SampleTemplateItem(
            id = "hello_world",
            title = "Hello World Starter",
            description = "Minimal Kotlin Android starter project with GitHub Actions workflow.",
            icon = Icons.Default.Code
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        text = "Choose Sample Project",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                templates.forEach { template ->
                    val isSelected = selectedTemplate == template.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTemplate = template.id
                                appNameInput = when (template.id) {
                                    "compose_counter" -> "Jetpack Counter App"
                                    "todo_app" -> "Task Master App"
                                    else -> "Hello World App"
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                            Icon(
                                imageVector = template.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = appNameInput,
                    onValueChange = { appNameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("App Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (appNameInput.isNotBlank()) {
                                onSelectSample(selectedTemplate, appNameInput)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compile Sample")
                    }
                }
            }
        }
    }
}
