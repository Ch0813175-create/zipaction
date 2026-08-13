package com.example.data.local

import androidx.room.*
import com.example.data.model.AppProject
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM app_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<AppProject>>

    @Query("SELECT * FROM app_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): AppProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: AppProject): Long

    @Update
    suspend fun updateProject(project: AppProject)

    @Query("DELETE FROM app_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("UPDATE app_projects SET status = :status, buildProgress = :progress, downloadUrl = :downloadUrl, workflowRunId = :workflowRunId, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectStatus(
        id: Long,
        status: String,
        progress: Int,
        downloadUrl: String?,
        workflowRunId: Long,
        errorMessage: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
}
