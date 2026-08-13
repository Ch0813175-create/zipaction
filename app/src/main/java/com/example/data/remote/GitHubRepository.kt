package com.example.data.remote

import android.util.Base64
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GitHubRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val apiService: GitHubApiService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(GitHubApiService::class.java)

    private fun getAuthHeader(token: String): String {
        val cleanToken = token.trim()
        return if (cleanToken.startsWith("token ", ignoreCase = true) || cleanToken.startsWith("Bearer ", ignoreCase = true)) {
            cleanToken
        } else {
            "Bearer $cleanToken"
        }
    }

    suspend fun validateToken(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentUser(getAuthHeader(token))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("GitHub Token invalid or expired (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ensureRepository(token: String, repoName: String): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val auth = getAuthHeader(token)
            val userResponse = apiService.getCurrentUser(auth)
            if (!userResponse.isSuccessful || userResponse.body() == null) {
                return@withContext Result.failure(Exception("Failed to fetch user credentials"))
            }
            val owner = userResponse.body()!!.login

            // Check if repo already exists
            val repoCheck = apiService.getRepository(auth, owner, repoName)
            if (repoCheck.isSuccessful && repoCheck.body() != null) {
                return@withContext Result.success(repoCheck.body()!!)
            }

            // Create repository if it doesn't exist
            val createResponse = apiService.createRepository(
                auth,
                CreateRepoRequest(
                    name = repoName,
                    description = "Automated Android app project created by App Compiler",
                    private = false,
                    autoInit = true
                )
            )

            if (createResponse.isSuccessful && createResponse.body() != null) {
                Result.success(createResponse.body()!!)
            } else {
                Result.failure(Exception("Could not create repository $repoName: ${createResponse.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads code files to GitHub repository using Git Trees and Commit API
     */
    suspend fun uploadCodeFiles(
        token: String,
        owner: String,
        repoName: String,
        files: Map<String, ByteArray>,
        onProgress: (Int, String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val auth = getAuthHeader(token)
            onProgress(10, "Creating Git Blobs for ${files.size} files...")

            val treeItems = mutableListOf<TreeItem>()
            var processed = 0

            for ((path, content) in files) {
                val base64Content = Base64.encodeToString(content, Base64.NO_WRAP)
                val blobResponse = apiService.createBlob(
                    auth, owner, repoName,
                    CreateBlobRequest(content = base64Content, encoding = "base64")
                )

                if (blobResponse.isSuccessful && blobResponse.body() != null) {
                    val blobSha = blobResponse.body()!!.sha
                    val isExecutable = path == "gradlew" || path.endsWith(".sh")
                    val mode = if (isExecutable) "100755" else "100644"
                    treeItems.add(TreeItem(path = path, mode = mode, type = "blob", sha = blobSha))
                } else {
                    return@withContext Result.failure(Exception("Failed to create blob for $path"))
                }

                processed++
                val progressPercent = 10 + ((processed.toFloat() / files.size) * 40).toInt()
                onProgress(progressPercent, "Processed $processed / ${files.size} files")
            }

            onProgress(55, "Fetching main branch reference...")
            var branchRef = "heads/main"
            var refResponse = apiService.getRef(auth, owner, repoName, branchRef)
            if (!refResponse.isSuccessful) {
                branchRef = "heads/master"
                refResponse = apiService.getRef(auth, owner, repoName, branchRef)
            }

            val parentCommitSha = if (refResponse.isSuccessful && refResponse.body() != null) {
                refResponse.body()!!.shaObject.sha
            } else {
                null
            }

            onProgress(65, "Creating Git Tree...")
            val treeResponse = apiService.createTree(
                auth, owner, repoName,
                CreateTreeRequest(baseTree = parentCommitSha, tree = treeItems)
            )

            if (!treeResponse.isSuccessful || treeResponse.body() == null) {
                return@withContext Result.failure(Exception("Failed to create Git Tree: ${treeResponse.errorBody()?.string()}"))
            }
            val treeSha = treeResponse.body()!!.sha

            onProgress(75, "Creating Commit...")
            val parents = if (parentCommitSha != null) listOf(parentCommitSha) else emptyList()
            val commitResponse = apiService.createCommit(
                auth, owner, repoName,
                CreateCommitRequest(
                    message = "Automated compile build triggered by App Compiler",
                    tree = treeSha,
                    parents = parents
                )
            )

            if (!commitResponse.isSuccessful || commitResponse.body() == null) {
                return@withContext Result.failure(Exception("Failed to create commit"))
            }
            val newCommitSha = commitResponse.body()!!.sha

            onProgress(85, "Updating branch head...")
            apiService.updateRef(
                auth, owner, repoName, branchRef,
                UpdateRefRequest(sha = newCommitSha, force = true)
            )

            onProgress(90, "Code successfully uploaded! SHA: ${newCommitSha.take(7)}")
            Result.success(newCommitSha)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLatestWorkflowRun(
        token: String,
        owner: String,
        repoName: String
    ): Result<WorkflowRun?> = withContext(Dispatchers.IO) {
        try {
            val auth = getAuthHeader(token)
            val response = apiService.getWorkflowRuns(auth, owner, repoName)
            if (response.isSuccessful && response.body() != null) {
                val runs = response.body()!!.workflowRuns
                Result.success(runs.firstOrNull())
            } else {
                Result.failure(Exception("Failed to fetch workflow runs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchArtifacts(
        token: String,
        owner: String,
        repoName: String,
        runId: Long
    ): Result<List<Artifact>> = withContext(Dispatchers.IO) {
        try {
            val auth = getAuthHeader(token)
            val response = apiService.getArtifacts(auth, owner, repoName, runId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.artifacts)
            } else {
                Result.failure(Exception("Failed to fetch artifacts for run $runId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWorkflowRunJobs(
        token: String,
        owner: String,
        repoName: String,
        runId: Long
    ): Result<List<Job>> = withContext(Dispatchers.IO) {
        try {
            val auth = getAuthHeader(token)
            val response = apiService.getWorkflowRunJobs(auth, owner, repoName, runId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.jobs)
            } else {
                Result.failure(Exception("Failed to fetch jobs for run $runId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
