package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppProject
import com.example.ui.components.GitHubTokenCard
import com.example.ui.components.LiveActionConsoleModal
import com.example.ui.components.OnboardingGuideModal
import com.example.ui.components.ProjectItemCard
import com.example.ui.components.SampleProjectPickerModal
import com.example.ui.components.UploadZipModal
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val uploadingStatus by viewModel.uploadingStatus.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()
    val liveJobs by viewModel.liveJobs.collectAsStateWithLifecycle()
    val isLoadingJobs by viewModel.isLoadingJobs.collectAsStateWithLifecycle()

    var showUploadZipModal by remember { mutableStateOf(false) }
    var showSampleModal by remember { mutableStateOf(false) }
    var showGuideModal by remember { mutableStateOf(false) }
    var selectedConsoleProject by remember { mutableStateOf<AppProject?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "App Compiler",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "GitHub Actions Auto Build",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSampleModal = true }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Sample Template",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showUploadZipModal = true },
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                text = { Text("Upload .ZIP App") },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("upload_zip_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            // GitHub Access Token configuration card
            item {
                GitHubTokenCard(
                    settingsState = settingsState,
                    onSaveToken = { token -> viewModel.saveToken(token) },
                    onOpenGuide = { showGuideModal = true }
                )
            }

            // Quick Actions Banner
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BuildCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Compile App Code to APK",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Upload any Android project .zip file. The app automatically pushes code to GitHub, executes GitHub Actions build, and gives you a direct APK download icon next to the app name!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showUploadZipModal = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = settingsState.user != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload ZIP", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showSampleModal = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = settingsState.user != null
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Try Sample", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Compiled Apps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${projects.size} Apps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Empty State if no projects exist
            if (projects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No Compiled Apps Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upload a .zip file containing your Android code or click 'Try Sample' to trigger your first GitHub Actions compilation!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = projects,
                    key = { it.id }
                ) { project ->
                    ProjectItemCard(
                        project = project,
                        onRefreshStatus = { proj -> viewModel.pollProjectStatusManually(proj) },
                        onDeleteProject = { id, deleteFromGitHub -> viewModel.deleteProject(id, deleteFromGitHub) },
                        onOpenConsole = { proj ->
                            selectedConsoleProject = proj
                            viewModel.fetchLiveJobsForProject(proj)
                        }
                    )
                }
            }
        }
    }

    // Modal dialogs
    if (showUploadZipModal) {
        UploadZipModal(
            uploadStatus = uploadingStatus,
            uploadProgress = uploadProgress,
            onDismiss = {
                showUploadZipModal = false
                viewModel.clearUploadStatus()
            },
            onUploadZip = { uri, appName, customRepo ->
                viewModel.uploadZipProject(
                    context = viewModel.getApplication(),
                    zipUri = uri,
                    appName = appName,
                    customRepoName = customRepo
                )
            },
            onOpenSamplePicker = {
                showUploadZipModal = false
                showSampleModal = true
            }
        )
    }

    if (showSampleModal) {
        SampleProjectPickerModal(
            onDismiss = { showSampleModal = false },
            onSelectSample = { templateType, appName ->
                viewModel.uploadSampleProject(templateType, appName)
            }
        )
    }

    if (showGuideModal) {
        OnboardingGuideModal(
            onDismiss = { showGuideModal = false }
        )
    }

    selectedConsoleProject?.let { proj ->
        LiveActionConsoleModal(
            project = proj,
            jobs = liveJobs,
            isLoadingJobs = isLoadingJobs,
            onDismiss = { selectedConsoleProject = null },
            onRefresh = { viewModel.fetchLiveJobsForProject(proj) },
            onTriggerAutoFix = {
                viewModel.triggerAutoFixAndRecompile(proj)
                selectedConsoleProject = null
            }
        )
    }
}
