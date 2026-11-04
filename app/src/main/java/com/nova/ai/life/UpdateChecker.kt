package com.nova.ai.life

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object UpdateChecker {

    private const val VERSION_URL = "https://raw.githubusercontent.com/nova-ai/nova-singularity/main/version.json"
    private val client = OkHttpClient()

    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(VERSION_URL).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val latestVersion = json.getString("version")
            val latestCode = json.getInt("versionCode")
            val apkUrl = json.getString("apkUrl")
            val sha256 = json.optString("sha256", "")
            val changelog = json.optString("changelog", "Enhancements & stability fixes.")

            val currentCode = 10 // Phase 10 Singularity

            if (latestCode > currentCode) {
                UpdateInfo(latestVersion, latestCode, apkUrl, sha256, changelog)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun downloadAndInstall(context: Context, updateInfo: UpdateInfo) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(updateInfo.apkUrl)

        val request = DownloadManager.Request(uri)
            .setTitle("Nova AI Update ${updateInfo.version}")
            .setDescription("Downloading latest Singularity build...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "nova-update-${updateInfo.version}.apk")

        dm.enqueue(request)
    }

    fun verifyChecksum(file: File, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank()) return true
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val fis = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            fis.close()
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            hex.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}

data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val apkUrl: String,
    val sha256: String,
    val changelog: String
)
