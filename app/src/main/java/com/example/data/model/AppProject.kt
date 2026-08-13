package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BuildStatus {
    QUEUED,
    UPLOADING,
    TRIGGERED,
    BUILDING,
    SUCCESS,
    FAILED
}

@Entity(tableName = "app_projects")
data class AppProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appName: String,
    val repoOwner: String,
    val repoName: String,
    val githubUrl: String,
    val zipSizeFormatted: String = "",
    val status: String = BuildStatus.QUEUED.name,
    val buildProgress: Int = 0,
    val downloadUrl: String? = null,
    val artifactName: String? = null,
    val workflowRunId: Long = 0,
    val commitSha: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
