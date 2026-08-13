package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppProject
import com.example.data.model.BuildStatus
import com.example.data.model.GitHubUser
import com.example.data.model.Job as GitHubJob
import com.example.data.remote.GitHubRepository
import com.example.util.ZipUtils
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GitHubSettingsState(
    val token: String = "",
    val user: GitHubUser? = null,
    val isValidating: Boolean = false,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val projectDao = db.projectDao()
    private val gitHubRepo = GitHubRepository()

    private val prefs = application.getSharedPreferences("app_compiler_prefs", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(GitHubSettingsState())
    val settingsState: StateFlow<GitHubSettingsState> = _settingsState.asStateFlow()

    val projects: StateFlow<List<AppProject>> = projectDao.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uploadingStatus = MutableStateFlow<String?>(null)
    val uploadingStatus: StateFlow<String?> = _uploadingStatus.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    private val activePollingJobs = mutableMapOf<Long, CoroutineJob>()

    init {
        // Load saved GitHub token if available
        val savedToken = prefs.getString("github_token", "") ?: ""
        if (savedToken.isNotEmpty()) {
            _settingsState.update { it.copy(token = savedToken) }
            validateGitHubToken(savedToken)
        }
    }

    fun saveToken(token: String) {
        val cleanToken = token.trim()
        prefs.edit().putString("github_token", cleanToken).apply()
        _settingsState.update { it.copy(token = cleanToken) }
        validateGitHubToken(cleanToken)
    }

    fun validateGitHubToken(token: String) {
        if (token.isBlank()) {
            _settingsState.update { it.copy(user = null, isValidating = false, error = "Token is empty") }
            return
        }

        viewModelScope.launch {
            _settingsState.update { it.copy(isValidating = true, error = null) }
            val result = gitHubRepo.validateToken(token)
            result.onSuccess { user ->
                _settingsState.update { it.copy(user = user, isValidating = false, error = null) }
            }.onFailure { err ->
                _settingsState.update { it.copy(user = null, isValidating = false, error = err.localizedMessage ?: "Invalid token") }
            }
        }
    }

    fun uploadZipProject(context: Context, zipUri: Uri, appName: String, customRepoName: String?) {
        val token = _settingsState.value.token
        if (token.isBlank()) {
            _settingsState.update { it.copy(error = "Please configure your GitHub Personal Access Token first") }
            return
        }

        viewModelScope.launch {
            try {
                _uploadingStatus.value = "Extracting ZIP and injecting GitHub Actions..."
                _uploadProgress.value = 5

                val (fileMap, diagnosticReports) = ZipUtils.extractZipAndInjectWorkflow(context, zipUri, appName)
                if (fileMap.isEmpty()) {
                    _uploadingStatus.value = "Error: ZIP file is empty or unreadable"
                    return@launch
                }

                val user = _settingsState.value.user
                val owner = user?.login ?: "github-user"
                val repoName = (customRepoName?.takeIf { it.isNotBlank() } ?: appName)
                    .lowercase()
                    .replace(Regex("[^a-z0-9_-]"), "-")

                val totalSize = fileMap.values.sumOf { it.size.toLong() }
                val formattedSize = ZipUtils.formatSize(totalSize)

                val newProject = AppProject(
                    appName = appName,
                    repoOwner = owner,
                    repoName = repoName,
                    githubUrl = "https://github.com/$owner/$repoName",
                    zipSizeFormatted = formattedSize,
                    status = BuildStatus.QUEUED.name,
                    buildProgress = 10
                )

                val projectId = projectDao.insertProject(newProject)

                executeUploadPipeline(projectId, token, owner, repoName, fileMap)

            } catch (e: Exception) {
                _uploadingStatus.value = "Failed: ${e.localizedMessage}"
            }
        }
    }

    fun uploadSampleProject(templateType: String, appName: String) {
        val token = _settingsState.value.token
        if (token.isBlank()) {
            _settingsState.update { it.copy(error = "Please configure your GitHub Personal Access Token first") }
            return
        }

        viewModelScope.launch {
            try {
                _uploadingStatus.value = "Generating sample Android project files..."
                _uploadProgress.value = 5

                val fileMap = ZipUtils.getSampleProjectFiles(templateType)
                val user = _settingsState.value.user
                val owner = user?.login ?: "github-user"
                val repoName = appName.lowercase().replace(Regex("[^a-z0-9_-]"), "-")

                val totalSize = fileMap.values.sumOf { it.size.toLong() }
                val formattedSize = ZipUtils.formatSize(totalSize)

                val newProject = AppProject(
                    appName = appName,
                    repoOwner = owner,
                    repoName = repoName,
                    githubUrl = "https://github.com/$owner/$repoName",
                    zipSizeFormatted = formattedSize,
                    status = BuildStatus.QUEUED.name,
                    buildProgress = 10
                )

                val projectId = projectDao.insertProject(newProject)

                executeUploadPipeline(projectId, token, owner, repoName, fileMap)

            } catch (e: Exception) {
                _uploadingStatus.value = "Failed: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun executeUploadPipeline(
        projectId: Long,
        token: String,
        owner: String,
        repoName: String,
        files: Map<String, ByteArray>
    ) {
        _uploadingStatus.value = "Ensuring GitHub repository exists..."
        _uploadProgress.value = 15

        projectDao.updateProjectStatus(
            id = projectId,
            status = BuildStatus.UPLOADING.name,
            progress = 15,
            downloadUrl = null,
            workflowRunId = 0,
            errorMessage = null
        )

        val repoResult = gitHubRepo.ensureRepository(token, repoName)
        if (repoResult.isFailure) {
            val errorMsg = repoResult.exceptionOrNull()?.localizedMessage ?: "Failed to prepare repository"
            projectDao.updateProjectStatus(projectId, BuildStatus.FAILED.name, 0, null, 0, errorMsg)
            _uploadingStatus.value = "Error: $errorMsg"
            return
        }

        _uploadingStatus.value = "Uploading files to GitHub repository..."

        val uploadResult = gitHubRepo.uploadCodeFiles(token, owner, repoName, files) { progress, statusMsg ->
            _uploadProgress.value = progress
            _uploadingStatus.value = statusMsg
            viewModelScope.launch {
                projectDao.updateProjectStatus(projectId, BuildStatus.UPLOADING.name, progress, null, 0, null)
            }
        }

        if (uploadResult.isFailure) {
            val errorMsg = uploadResult.exceptionOrNull()?.localizedMessage ?: "Failed to upload files"
            projectDao.updateProjectStatus(projectId, BuildStatus.FAILED.name, 0, null, 0, errorMsg)
            _uploadingStatus.value = "Upload failed: $errorMsg"
            return
        }

        val commitSha = uploadResult.getOrNull()

        _uploadingStatus.value = "GitHub Actions Workflow triggered! Monitoring build..."
        _uploadProgress.value = 90

        projectDao.updateProjectStatus(
            id = projectId,
            status = BuildStatus.TRIGGERED.name,
            progress = 90,
            downloadUrl = null,
            workflowRunId = 0,
            errorMessage = null
        )

        // Reset dialog status after successful launch
        delay(1500)
        _uploadingStatus.value = null
        _uploadProgress.value = 0

        // Start background polling for workflow completion
        startPollingWorkflow(projectId, token, owner, repoName)
    }

    fun pollProjectStatusManually(project: AppProject) {
        val token = _settingsState.value.token
        if (token.isBlank()) return
        startPollingWorkflow(project.id, token, project.repoOwner, project.repoName)
    }

    private fun startPollingWorkflow(projectId: Long, token: String, owner: String, repoName: String) {
        activePollingJobs[projectId]?.cancel()

        activePollingJobs[projectId] = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 40 // Poll for up to 10 minutes (40 * 15 sec)

            while (attempts < maxAttempts) {
                delay(if (attempts == 0) 3000 else 15000)
                attempts++

                val runResult = gitHubRepo.fetchLatestWorkflowRun(token, owner, repoName)
                if (runResult.isSuccess && runResult.getOrNull() != null) {
                    val run = runResult.getOrNull()!!
                    val status = run.status // queued, in_progress, completed
                    val conclusion = run.conclusion // success, failure, cancelled

                    when (status) {
                        "queued" -> {
                            projectDao.updateProjectStatus(
                                projectId, BuildStatus.BUILDING.name, 25,
                                downloadUrl = run.htmlUrl,
                                workflowRunId = run.id,
                                errorMessage = "Workflow queued on GitHub Actions..."
                            )
                        }
                        "in_progress" -> {
                            projectDao.updateProjectStatus(
                                projectId, BuildStatus.BUILDING.name, 60,
                                downloadUrl = run.htmlUrl,
                                workflowRunId = run.id,
                                errorMessage = "Compiling Android APK on GitHub Actions..."
                            )
                        }
                        "completed" -> {
                            if (conclusion == "success") {
                                // Try fetching compiled artifact download URL
                                val artifactsResult = gitHubRepo.fetchArtifacts(token, owner, repoName, run.id)
                                val downloadUrl = if (artifactsResult.isSuccess && artifactsResult.getOrNull()?.isNotEmpty() == true) {
                                    val artifact = artifactsResult.getOrNull()!!.first()
                                    // GitHub API artifact download URL or action run URL
                                    artifact.archiveDownloadUrl
                                } else {
                                    // Direct link to GitHub Actions run artifacts page
                                    run.htmlUrl
                                }

                                projectDao.updateProjectStatus(
                                    projectId, BuildStatus.SUCCESS.name, 100,
                                    downloadUrl = downloadUrl ?: run.htmlUrl,
                                    workflowRunId = run.id,
                                    errorMessage = null
                                )
                            } else {
                                projectDao.updateProjectStatus(
                                    projectId, BuildStatus.FAILED.name, 0,
                                    downloadUrl = run.htmlUrl,
                                    workflowRunId = run.id,
                                    errorMessage = "Build $conclusion on GitHub Actions"
                                )
                            }
                            break // Poll completed
                        }
                    }
                }
            }
        }
    }

    private val _liveJobs = MutableStateFlow<List<GitHubJob>>(emptyList())
    val liveJobs: StateFlow<List<GitHubJob>> = _liveJobs.asStateFlow()

    private val _isLoadingJobs = MutableStateFlow(false)
    val isLoadingJobs: StateFlow<Boolean> = _isLoadingJobs.asStateFlow()

    fun fetchLiveJobsForProject(project: AppProject) {
        val token = _settingsState.value.token
        if (token.isBlank() || project.workflowRunId == 0L) return

        viewModelScope.launch {
            _isLoadingJobs.value = true
            val jobsResult = gitHubRepo.fetchWorkflowRunJobs(
                token = token,
                owner = project.repoOwner,
                repoName = project.repoName,
                runId = project.workflowRunId
            )
            _isLoadingJobs.value = false
            if (jobsResult.isSuccess) {
                _liveJobs.value = jobsResult.getOrNull() ?: emptyList()
            }
        }
    }

    fun triggerAutoFixAndRecompile(project: AppProject) {
        val token = _settingsState.value.token
        if (token.isBlank()) return

        viewModelScope.launch {
            try {
                _uploadingStatus.value = "🤖 Applying self-healing build patches to GitHub..."
                _uploadProgress.value = 30

                // Patch workflow file on remote GitHub repo
                val patchedFiles = mapOf(
                    ZipUtils.WORKFLOW_PATH to ZipUtils.DEFAULT_WORKFLOW_YAML.toByteArray(Charsets.UTF_8)
                )

                val uploadResult = gitHubRepo.uploadCodeFiles(token, project.repoOwner, project.repoName, patchedFiles) { prog, msg ->
                    _uploadProgress.value = prog
                    _uploadingStatus.value = msg
                }

                if (uploadResult.isSuccess) {
                    projectDao.updateProjectStatus(
                        id = project.id,
                        status = BuildStatus.QUEUED.name,
                        progress = 20,
                        downloadUrl = null,
                        workflowRunId = 0,
                        errorMessage = "Auto-fix applied! Re-triggering build..."
                    )
                    // Restart polling
                    pollProjectStatusManually(project)
                }
            } catch (e: Exception) {
                _uploadingStatus.value = "Auto-fix failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteProject(projectId: Long, deleteFromGitHub: Boolean = false) {
        viewModelScope.launch {
            activePollingJobs[projectId]?.cancel()
            projectDao.deleteProjectById(projectId)
        }
    }

    fun clearUploadStatus() {
        _uploadingStatus.value = null
        _uploadProgress.value = 0
    }
}
