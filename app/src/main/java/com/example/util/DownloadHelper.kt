package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object DownloadHelper {

    fun downloadOrOpen(context: Context, downloadUrl: String, appName: String) {
        try {
            val uri = Uri.parse(downloadUrl)

            // If the URL ends with .apk or is a direct binary artifact
            if (downloadUrl.endsWith(".apk", ignoreCase = true) || downloadUrl.contains("/artifacts/")) {
                val request = DownloadManager.Request(uri).apply {
                    setTitle("$appName APK")
                    setDescription("Downloading compiled Android APK from GitHub Actions")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "${appName.replace(" ", "_")}-debug.apk"
                    )
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }

                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                if (downloadManager != null) {
                    downloadManager.enqueue(request)
                    Toast.makeText(context, "Downloading $appName APK...", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            // Fallback: Open in Web Browser or GitHub App
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening download link in browser...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open download link: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
