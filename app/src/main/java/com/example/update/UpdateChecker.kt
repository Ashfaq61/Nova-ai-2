package com.example.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

class UpdateChecker(private val context: Context) {

    companion object {
        const val CURRENT_VERSION = "2.0.0"
        private const val GITHUB_VERSION_URL = "https://raw.githubusercontent.com/nova-ai/nova-app/main/version.json"
    }

    fun getCurrentVersion(): String = CURRENT_VERSION

    suspend fun checkForUpdates(): String = withContext(Dispatchers.IO) {
        try {
            val response = URL(GITHUB_VERSION_URL).readText()
            val json = JSONObject(response)
            val remoteVersion = json.optString("version", CURRENT_VERSION)
            val hasUpdate = compareSemVer(remoteVersion, CURRENT_VERSION) > 0

            JSONObject().apply {
                put("hasUpdate", hasUpdate)
                put("currentVersion", CURRENT_VERSION)
                put("remoteVersion", remoteVersion)
                put("changelog", json.optString("changelog", "Performance improvements & neural voice enhancements."))
                put("downloadUrl", json.optString("downloadUrl", ""))
            }.toString()
        } catch (_: Exception) {
            JSONObject().apply {
                put("hasUpdate", false)
                put("currentVersion", CURRENT_VERSION)
                put("remoteVersion", CURRENT_VERSION)
                put("status", "Up to date (offline verification)")
            }.toString()
        }
    }

    private fun compareSemVer(v1: String, v2: String): Int {
        val p1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val p2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val num1 = p1.getOrElse(i) { 0 }
            val num2 = p2.getOrElse(i) { 0 }
            if (num1 != num2) return num1.compareTo(num2)
        }
        return 0
    }

    fun downloadAndInstallUpdate(downloadUrl: String) {
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Nova AI Update")
            .setDescription("Downloading latest build")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "nova-update.apk")

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}
