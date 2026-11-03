package com.example.pclink

import android.content.Context
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.text.format.Formatter
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

class PcLinkServer(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    var pairingCode: String = generatePairingCode()
        private set

    companion object {
        const val PORT = 8765
        private const val TAG = "NovaPcLink"
    }

    private fun generatePairingCode(): String {
        return String.format("%06d", Random.nextInt(100000, 999999))
    }

    fun start(): Boolean {
        if (isRunning) return true
        return try {
            serverSocket = ServerSocket(PORT)
            isRunning = true
            pairingCode = generatePairingCode()

            CoroutineScope(Dispatchers.IO).launch {
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        handleClient(client)
                    } catch (_: Exception) {
                        break
                    }
                }
            }
            Log.d(TAG, "PC Link server started on port $PORT with pairing code $pairingCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PC Link: ${e.message}")
            false
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    fun getLocalIp(): String {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    private fun handleClient(socket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val requestLine = reader.readLine() ?: return@launch
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: "GET"
                val path = parts.getOrNull(1) ?: "/"

                // Read headers
                var contentLength = 0
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                // Read body if POST
                var body = ""
                if (method == "POST" && contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    reader.read(buffer, 0, contentLength)
                    body = String(buffer)
                }

                when {
                    path == "/" -> {
                        val html = buildDashboardHtml()
                        writer.println("HTTP/1.1 200 OK")
                        writer.println("Content-Type: text/html; charset=utf-8")
                        writer.println("Content-Length: ${html.toByteArray().size}")
                        writer.println("Access-Control-Allow-Origin: *")
                        writer.println()
                        writer.print(html)
                    }
                    path == "/api/status" -> {
                        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                        val bat = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 88
                        val json = JSONObject().apply {
                            put("ip", getLocalIp())
                            put("port", PORT)
                            put("battery", bat)
                            put("pairingCode", pairingCode)
                            put("activeApp", "Nova AI Core")
                            put("status", "ONLINE")
                        }.toString()

                        writer.println("HTTP/1.1 200 OK")
                        writer.println("Content-Type: application/json")
                        writer.println("Access-Control-Allow-Origin: *")
                        writer.println("Content-Length: ${json.toByteArray().size}")
                        writer.println()
                        writer.print(json)
                    }
                    path == "/api/cmd" && method == "POST" -> {
                        val resp = JSONObject().apply {
                            put("status", "RECEIVED")
                            put("command", body)
                        }.toString()

                        writer.println("HTTP/1.1 200 OK")
                        writer.println("Content-Type: application/json")
                        writer.println("Access-Control-Allow-Origin: *")
                        writer.println("Content-Length: ${resp.toByteArray().size}")
                        writer.println()
                        writer.print(resp)
                    }
                    else -> {
                        writer.println("HTTP/1.1 404 Not Found")
                        writer.println("Content-Length: 0")
                        writer.println()
                    }
                }
                writer.flush()
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun buildDashboardHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Nova AI — PC Link Command Center</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { background: #07070d; color: #fff; font-family: -apple-system, sans-serif; padding: 24px; margin: 0; }
                    .card { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.15); border-radius: 16px; padding: 20px; max-width: 520px; margin: 0 auto; }
                    h1 { color: #00e0ff; font-size: 22px; margin-top: 0; }
                    .badge { display: inline-block; padding: 4px 10px; border-radius: 20px; background: #7c5cff; font-size: 12px; margin-bottom: 16px; }
                    button { background: linear-gradient(135deg, #7c5cff, #00e0ff); border: none; color: #fff; padding: 12px 18px; border-radius: 10px; font-weight: bold; cursor: pointer; width: 100%; margin-top: 10px; }
                    input { width: 100%; padding: 12px; box-sizing: border-box; background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.2); border-radius: 8px; color: #fff; margin-bottom: 10px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>⚡ Nova AI &bull; PC Link</h1>
                    <span class="badge">Paired on Port $PORT &bull; Code: $pairingCode</span>
                    <p>Transmit instant commands and control mobile automation directly from your desktop browser.</p>
                    <input type="text" id="cmdInput" placeholder="Enter voice prompt or command...">
                    <button onclick="sendCmd()">Transmit to Nova</button>
                    <div id="log" style="margin-top:16px; font-size:13px; color:#aaa;">Ready.</div>
                </div>
                <script>
                    function sendCmd() {
                        const val = document.getElementById('cmdInput').value;
                        fetch('/api/cmd', { method: 'POST', body: JSON.stringify({ type: 'chat', prompt: val }) })
                            .then(r => r.json())
                            .then(d => { document.getElementById('log').innerText = 'Dispatched: ' + JSON.stringify(d); });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
