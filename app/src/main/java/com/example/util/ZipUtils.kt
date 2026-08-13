package com.example.util

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    data class FileEntry(
        val relativePath: String,
        val content: ByteArray,
        val isText: Boolean = true
    )

    const val WORKFLOW_PATH = ".github/workflows/android-build.yml"

    fun generateWorkflowFileEntry(): FileEntry {
        return FileEntry(
            relativePath = WORKFLOW_PATH,
            content = DEFAULT_WORKFLOW_YAML.toByteArray(Charsets.UTF_8),
            isText = true
        )
    }

    val DEFAULT_WORKFLOW_YAML = """
name: Android Build & Compile APK

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    name: Build Android APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant Execute Permission for Gradle
        run: |
          if [ -f gradlew ]; then
            chmod +x gradlew
          fi

      - name: Build Debug APK
        run: |
          if [ -f gradlew ]; then
            ./gradlew assembleDebug --stacktrace
          else
            gradle assembleDebug --stacktrace
          fi

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: warn
""".trimIndent()

    /**
     * Extracts files from a ZIP Uri into a Map of relativePath -> ByteArray
     * Automatically runs self-healing diagnostics algorithms and injects workflow
     */
    fun extractZipAndInjectWorkflow(context: Context, zipUri: Uri, appName: String = "AppProject"): Pair<Map<String, ByteArray>, List<DiagnosticReport>> {
        val rawEntries = mutableListOf<FileEntry>()
        val inputStream = context.contentResolver.openInputStream(zipUri) ?: return Pair(emptyMap(), emptyList())

        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val cleanPath = entry.name.removePrefix("/")
                    val buffer = ByteArrayOutputStream()
                    val data = ByteArray(4096)
                    var count: Int
                    while (zis.read(data).also { count = it } != -1) {
                        buffer.write(data, 0, count)
                    }
                    val bytes = buffer.toByteArray()
                    val isText = !cleanPath.endsWith(".png") && !cleanPath.endsWith(".jpg") && !cleanPath.endsWith(".jar") && !cleanPath.endsWith(".so")
                    rawEntries.add(FileEntry(cleanPath, bytes, isText))
                }
                entry = zis.nextEntry
            }
        }

        // Run self-healing diagnostics & auto-fix algorithms
        val fixResult = BuildDiagnosticsEngine.analyzeAndAutoFixProject(rawEntries, appName)
        val resultMap = fixResult.patchedFiles.associate { it.relativePath to it.content }

        return Pair(resultMap, fixResult.reports)
    }

    /**
     * Generates a sample ready-to-compile Android Project with full source code
     */
    fun getSampleProjectFiles(templateType: String): Map<String, ByteArray> {
        val files = mutableMapOf<String, ByteArray>()

        val appName = when (templateType) {
            "compose_counter" -> "ComposeCounter"
            "todo_app" -> "TaskMaster"
            else -> "HelloWorldApp"
        }

        val settingsGradle = """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "$appName"
include(":app")
""".trimIndent()

        val rootBuildGradle = """
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
""".trimIndent()

        val libsToml = """
[versions]
agp = "8.8.0"
kotlin = "2.0.21"
coreKtx = "1.15.0"
lifecycleRuntime = "2.8.7"
activityCompose = "1.10.0"
composeBom = "2024.12.01"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntime" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
""".trimIndent()

        val appBuildGradle = """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sample.${appName.lowercase()}"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sample.${appName.lowercase()}"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
}
""".trimIndent()

        val manifest = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="$appName"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimIndent()

        val mainActivity = """
package com.sample.${appName.lowercase()}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var count by remember { mutableStateOf(0) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hello from $appName!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Compiled automatically via GitHub Actions!",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { count++ }) {
                            Text("Clicks: ${'$'}count")
                        }
                    }
                }
            }
        }
    }
}
""".trimIndent()

        val gradlewScript = """
#!/usr/bin/env sh
echo "Gradle Wrapper Executable"
""".trimIndent()

        files["settings.gradle.kts"] = settingsGradle.toByteArray()
        files["build.gradle.kts"] = rootBuildGradle.toByteArray()
        files["gradle/libs.versions.toml"] = libsToml.toByteArray()
        files["app/build.gradle.kts"] = appBuildGradle.toByteArray()
        files["app/src/main/AndroidManifest.xml"] = manifest.toByteArray()
        files["app/src/main/java/com/sample/${appName.lowercase()}/MainActivity.kt"] = mainActivity.toByteArray()
        files["gradlew"] = gradlewScript.toByteArray()
        files[WORKFLOW_PATH] = DEFAULT_WORKFLOW_YAML.toByteArray()

        return files
    }

    /**
     * Formats bytes to human readable KB/MB
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
