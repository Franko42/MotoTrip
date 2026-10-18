package com.mototrip.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdateManager(
    private val context: Context,
    private val repoOwner: String = "Franko42",
    private val repoName: String = "MotoTrip"
) {

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val currentVersion = getCurrentVersionName()
            val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MotoTrip-Updater")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // No releases published yet in this repository
                return@runCatching AppUpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = currentVersion,
                    releaseTitle = "Sin actualizaciones",
                    releaseNotes = "Aún no se han publicado releases en el repositorio.",
                    apkDownloadUrl = null,
                    apkFileName = null,
                    apkSizeBytes = null,
                    isUpdateAvailable = false
                )
            }

            if (responseCode !in 200..299) {
                error("Error al consultar GitHub Releases (HTTP $responseCode)")
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            parseReleaseResponse(responseBody, currentVersion)
        }
    }

    suspend fun downloadApk(
        apkUrl: String,
        targetFileName: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val baseDir = context.getExternalFilesDir(null) ?: context.cacheDir
            val updatesDir = File(baseDir, "updates").apply { mkdirs() }
            val outputFile = File(updatesDir, targetFileName)
            val tempFile = File(updatesDir, "$targetFileName.tmp")

            var currentUrl = apkUrl
            var connection: HttpURLConnection
            var redirects = 0

            // Follow HTTP redirects (GitHub Releases redirect to AWS S3 / objects.githubusercontent.com)
            while (true) {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 25000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "MotoTrip-Updater")
                }

                val status = connection.responseCode
                if (status in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308)) {
                    val location = connection.getHeaderField("Location") ?: error("Redirect sin cabecera Location")
                    connection.disconnect()
                    currentUrl = location
                    redirects++
                    if (redirects > 8) error("Demasiados redireccionamientos de descarga.")
                    continue
                }
                break
            }

            if (connection.responseCode !in 200..299) {
                error("Fallo al descargar el archivo APK (HTTP ${connection.responseCode})")
            }

            val totalBytes = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { input: InputStream ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes
                            onProgress(progress.coerceIn(0f, 1f))
                        }
                    }
                }
            }

            if (outputFile.exists()) {
                outputFile.delete()
            }
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }

            outputFile.setReadable(true, false)
            outputFile
        }
    }

    fun startInstall(apkFile: File) {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            error("El archivo APK descargado no es válido o está vacío.")
        }
        apkFile.setReadable(true, false)

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        // Android 8.0+ Check permission for Unknown Sources installation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }

        // Grant explicit URI read permission to all candidate package installer activities
        val resolveInfoList = context.packageManager.queryIntentActivities(installIntent, 0)
        for (resolveInfo in resolveInfoList) {
            val targetPackage = resolveInfo.activityInfo.packageName
            context.grantUriPermission(targetPackage, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(installIntent)
    }

    companion object {
        fun parseReleaseResponse(jsonString: String, currentVersion: String): AppUpdateInfo {
            val json = JSONObject(jsonString)

            val rawTagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", rawTagName).ifBlank { rawTagName }
            val releaseNotes = json.optString("body", "Mejoras y correcciones generales.")

            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long? = null

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", null)
                        apkName = name
                        apkSize = asset.optLong("size", 0L).takeIf { it > 0 }
                        break
                    }
                }
            }

            val isAvailable = isNewerVersion(currentVersion, rawTagName) && !apkUrl.isNullOrBlank()

            return AppUpdateInfo(
                currentVersion = currentVersion,
                latestVersion = rawTagName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize,
                isUpdateAvailable = isAvailable
            )
        }

        fun isNewerVersion(current: String, remote: String): Boolean {
            val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")
            val cleanRemote = remote.trim().removePrefix("v").removePrefix("V")

            if (cleanRemote.isBlank()) return false
            if (cleanCurrent.isBlank()) return true
            if (cleanCurrent == cleanRemote) return false

            val currentParts = cleanCurrent.split(".", "-").mapNotNull { it.toIntOrNull() }
            val remoteParts = cleanRemote.split(".", "-").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(currentParts.size, remoteParts.size)
            for (i in 0 until maxLen) {
                val c = currentParts.getOrElse(i) { 0 }
                val r = remoteParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
