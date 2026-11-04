package com.nova.ai.life

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import com.nova.ai.ai.LlmManager
import com.nova.ai.services.NovaAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class PcLinkServer(private val context: Context, private val port: Int = 8765) {

    companion object {
        @Volatile
        private var instance: PcLinkServer? = null
        var isServerRunning: Boolean = false
            private set

        fun start(context: Context, port: Int = 8765) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = PcLinkServer(context.applicationContext, port).also {
                            it.start()
                        }
                    }
                }
            }
        }

        fun stop() {
            instance?.stop()
            instance = null
        }
    }

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        isServerRunning = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                // Server closed or port error
            } finally {
                isServerRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        isServerRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val out: OutputStream = socket.getOutputStream()

                val requestLine = reader.readLine() ?: return@launch
                val parts = requestLine.split(" ")
                val method = if (parts.isNotEmpty()) parts[0] else "GET"
                val path = if (parts.size > 1) parts[1] else "/"

                var contentLength = 0
                var line: String? = reader.readLine()
                while (!line.isNullOrBlank()) {
                    if (line.lowercase().startsWith("content-length:")) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                val body = if (contentLength > 0) {
                    val buf = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val r = reader.read(buf, readTotal, contentLength - readTotal)
                        if (r == -1) break
                        readTotal += r
                    }
                    String(buf, 0, readTotal)
                } else ""

                when {
                    path == "/" -> sendResponse(out, 200, "text/html", getDashboardHtml())
                    path == "/api/status" -> sendResponse(out, 200, "application/json", getStatusJson())
                    path.startsWith("/api/cmd") -> {
                        val responseJson = handleApiCommand(body)
                        sendResponse(out, 200, "application/json", responseJson)
                    }
                    else -> sendResponse(out, 404, "text/plain", "Not Found")
                }
            } catch (e: Exception) {
                // client socket error
            } finally {
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    private fun sendResponse(out: OutputStream, code: Int, contentType: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val statusText = if (code == 200) "OK" else "Error"
        val header = "HTTP/1.1 $code $statusText\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun getStatusJson(): String {
        val batt = try {
            context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        } catch (e: Exception) { -1 }

        return JSONObject()
            .put("device", Build.MODEL)
            .put("android", Build.VERSION.RELEASE)
            .put("battery", batt)
            .put("accessibility", NovaAccessibilityService.instance != null)
            .put("activePackage", NovaAccessibilityService.lastPkg ?: "None")
            .put("singularityPort", port)
            .toString()
    }

    private suspend fun handleApiCommand(body: String): String {
        return try {
            val json = JSONObject(body)
            val cmd = json.optString("cmd")

            when (cmd) {
                "tap" -> {
                    val x = json.optDouble("x", 500.0).toFloat()
                    val y = json.optDouble("y", 800.0).toFloat()
                    val success = NovaAccessibilityService.tap(x, y)
                    JSONObject().put("success", success).put("action", "tap").toString()
                }
                "swipe" -> {
                    val sx = json.optDouble("startX", 500.0).toFloat()
                    val sy = json.optDouble("startY", 1200.0).toFloat()
                    val ex = json.optDouble("endX", 500.0).toFloat()
                    val ey = json.optDouble("endY", 400.0).toFloat()
                    val success = NovaAccessibilityService.swipe(sx, sy, ex, ey)
                    JSONObject().put("success", success).put("action", "swipe").toString()
                }
                "launch" -> {
                    val pkg = json.optString("pkg")
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    val success = if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        true
                    } else false
                    JSONObject().put("success", success).put("pkg", pkg).toString()
                }
                "chat" -> {
                    val prompt = json.optString("text")
                    val reply = LlmManager.ask(prompt, "You are Nova AI responding via PC Link bridge.")
                    JSONObject().put("reply", reply).toString()
                }
                else -> JSONObject().put("error", "Unknown command: $cmd").toString()
            }
        } catch (e: Exception) {
            JSONObject().put("error", e.message ?: "Command failed").toString()
        }
    }

    private fun getDashboardHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <title>Nova AI • PC Link Terminal</title>
              <style>
                body { margin: 0; background: #07070d; color: #fff; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; }
                .card { background: rgba(255,255,255,0.05); border: 1px solid rgba(124,92,255,0.3); border-radius: 16px; padding: 24px; width: 440px; box-shadow: 0 8px 32px rgba(0,0,0,0.5); }
                h1 { margin-top: 0; font-size: 20px; color: #00e0ff; display: flex; align-items: center; gap: 8px; }
                .pill { display: inline-block; padding: 4px 10px; border-radius: 12px; background: rgba(0,255,163,0.15); color: #00ffa3; font-size: 12px; font-weight: bold; }
                .btn { background: #7c5cff; color: #fff; border: none; padding: 10px 16px; border-radius: 8px; cursor: pointer; font-weight: 600; width: 100%; margin-top: 10px; }
                .btn:hover { background: #9275ff; }
                input { width: 100%; padding: 10px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.2); background: rgba(0,0,0,0.4); color: #fff; box-sizing: border-box; margin-top: 8px; }
                #log { background: #000; border-radius: 8px; padding: 10px; font-family: monospace; font-size: 12px; height: 120px; overflow-y: auto; margin-top: 12px; color: #00ffa3; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>⚡ Nova AI Singularity PC Link <span class="pill">ONLINE</span></h1>
                <p style="color: #aaa; font-size: 13px;">Connected to Android Device Port 8765</p>
                <input type="text" id="cmdInput" placeholder="Type prompt or command...">
                <button class="btn" onclick="sendPrompt()">Dispatch to Nova Agent</button>
                <div id="log">[Terminal ready. Type command and dispatch.]</div>
              </div>
              <script>
                async function sendPrompt() {
                  const val = document.getElementById('cmdInput').value.trim();
                  if (!val) return;
                  const log = document.getElementById('log');
                  log.innerHTML += '<div>> ' + val + '</div>';
                  document.getElementById('cmdInput').value = '';
                  try {
                    const res = await fetch('/api/cmd', {
                      method: 'POST',
                      headers: {'Content-Type': 'application/json'},
                      body: JSON.stringify({cmd: 'chat', text: val})
                    });
                    const data = await res.json();
                    log.innerHTML += '<div style="color:#00e0ff;">Nova: ' + (data.reply || JSON.stringify(data)) + '</div>';
                    log.scrollTop = log.scrollHeight;
                  } catch(e) {
                    log.innerHTML += '<div style="color:#ff3d8b;">Error: ' + e.message + '</div>';
                  }
                }
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
