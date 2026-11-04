package com.nova.ai.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MeshNetworkService : Service() {

    companion object {
        private const val TAG = "NovaMeshService"
        private const val PORT = 8766
        private const val SECRET_PASSPHRASE = "NovaSingularityKey1234567890123" // 256-bit AES
        var isMeshActive: Boolean = false
            private set
        val connectedNodes = mutableListOf<String>()

        fun encrypt(plainText: String): String {
            return try {
                val key = SecretKeySpec(SECRET_PASSPHRASE.toByteArray(Charsets.UTF_8).copyOf(32), "AES")
                val iv = IvParameterSpec(ByteArray(16))
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key, iv)
                val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            } catch (e: Exception) {
                plainText
            }
        }

        fun decrypt(cipherText: String): String {
            return try {
                val key = SecretKeySpec(SECRET_PASSPHRASE.toByteArray(Charsets.UTF_8).copyOf(32), "AES")
                val iv = IvParameterSpec(ByteArray(16))
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, key, iv)
                val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
                String(cipher.doFinal(decoded), Charsets.UTF_8)
            } catch (e: Exception) {
                cipherText
            }
        }
    }

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        isMeshActive = true
        startTcpServer()
        Log.i(TAG, "Nova Mesh P2P server started on port $PORT")
    }

    private fun startTcpServer() {
        isRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(PORT)
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                // Server closed
            }
        }
    }

    private fun handleClient(client: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ip = client.inetAddress.hostAddress ?: "Unknown"
                if (!connectedNodes.contains(ip)) connectedNodes.add(ip)

                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val out: OutputStream = client.getOutputStream()

                val line = reader.readLine()
                if (!line.isNullOrBlank()) {
                    val decrypted = decrypt(line)
                    Log.d(TAG, "Mesh received from $ip: $decrypted")

                    // Respond with encrypted ack
                    val response = JSONObject()
                        .put("status", "SYNCHRONIZED")
                        .put("node", "Nova-Core")
                        .put("timestamp", System.currentTimeMillis())
                        .toString()

                    val encResponse = encrypt(response) + "\n"
                    out.write(encResponse.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Mesh client error", e)
            } finally {
                try { client.close() } catch (e: Exception) {}
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isMeshActive = false
        isRunning = false
        try { serverSocket?.close() } catch (e: Exception) {}
        serverSocket = null
        Log.i(TAG, "Nova Mesh P2P service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
