package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    @Json(name = "avatar_url") val avatarUrl: String?,
    val name: String?,
    @Json(name = "html_url") val htmlUrl: String?
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String = "Automated Android App build created by App Compiler",
    val private: Boolean = false,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "html_url") val htmlUrl: String,
    val owner: GitHubUserOwner
)

@JsonClass(generateAdapter = true)
data class GitHubUserOwner(
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class ShaObject(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class RefResponse(
    val ref: String,
    @Json(name = "object") val shaObject: ShaObject
)

@JsonClass(generateAdapter = true)
data class CreateBlobRequest(
    val content: String,
    val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class CreateBlobResponse(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class TreeItem(
    val path: String,
    val mode: String = "100644",
    val type: String = "blob",
    val sha: String? = null,
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateTreeRequest(
    @Json(name = "base_tree") val baseTree: String?,
    val tree: List<TreeItem>
)

@JsonClass(generateAdapter = true)
data class CreateTreeResponse(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>
)

@JsonClass(generateAdapter = true)
data class CreateCommitResponse(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = true
)

@JsonClass(generateAdapter = true)
data class WorkflowRun(
    val id: Long,
    val name: String?,
    @Json(name = "head_branch") val headBranch: String?,
    @Json(name = "head_sha") val headSha: String?,
    val status: String?, // queued, in_progress, completed
    val conclusion: String?, // success, failure, cancelled
    @Json(name = "html_url") val htmlUrl: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class WorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "workflow_runs") val workflowRuns: List<WorkflowRun>
)

@JsonClass(generateAdapter = true)
data class Artifact(
    val id: Long,
    val name: String,
    @Json(name = "size_in_bytes") val sizeInBytes: Long,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ArtifactsResponse(
    @Json(name = "total_count") val totalCount: Int,
    val artifacts: List<Artifact>
)

@JsonClass(generateAdapter = true)
data class JobStep(
    val name: String,
    val status: String, // queued, in_progress, completed
    val conclusion: String?, // success, failure, skipped
    val number: Int,
    @Json(name = "started_at") val startedAt: String?,
    @Json(name = "completed_at") val completedAt: String?
)

@JsonClass(generateAdapter = true)
data class Job(
    val id: Long,
    @Json(name = "run_id") val runId: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val steps: List<JobStep> = emptyList(),
    @Json(name = "html_url") val htmlUrl: String?
)

@JsonClass(generateAdapter = true)
data class JobsResponse(
    @Json(name = "total_count") val totalCount: Int,
    val jobs: List<Job>
)
