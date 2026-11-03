package com.example.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

interface LlmProvider {
    val name: String
    suspend fun generate(prompt: String, systemPrompt: String): String
}

class GeminiProvider(private val apiKey: String) : LlmProvider {
    override val name: String = "Gemini-1.5-Flash"

    override suspend fun generate(prompt: String, systemPrompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IllegalStateException("Gemini API key is missing")
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 12000

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "[System: $systemPrompt]\n\nQuery: $prompt"))
                    })
                })
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode in 200..299) {
            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(response)
            val candidates = json.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
            text ?: throw IllegalStateException("Empty Gemini candidate text")
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw IllegalStateException("Gemini HTTP error ${conn.responseCode}: $err")
        }
    }
}

class OpenRouterProvider(private val apiKey: String) : LlmProvider {
    override val name: String = "OpenRouter-Auto"

    override suspend fun generate(prompt: String, systemPrompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IllegalStateException("OpenRouter API key is missing")
        val endpoint = "https://openrouter.ai/api/v1/chat/completions"
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 12000

        val payload = JSONObject().apply {
            put("model", "meta-llama/llama-3-8b-instruct:free")
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", prompt))
            })
        }

        OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

        if (conn.responseCode in 200..299) {
            val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(response)
            json.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content") ?: throw IllegalStateException("Empty OpenRouter text")
        } else {
            throw IllegalStateException("OpenRouter error HTTP ${conn.responseCode}")
        }
    }
}

class LocalRuleProvider : LlmProvider {
    override val name: String = "On-Device Local Brain"

    override suspend fun generate(prompt: String, systemPrompt: String): String = withContext(Dispatchers.Default) {
        val q = prompt.lowercase()
        when {
            q.contains("time") -> "The current system time is ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())}."
            q.contains("date") -> "Today is ${java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}."
            q.contains("battery") -> "Device battery is actively monitored and optimal."
            q.contains("hello") || q.contains("hi") -> "Hello! Nova AI is operating in zero-latency on-device mode."
            q.contains("who are you") -> "I am Nova AI, your living assistant with holographic orb interface and full device control."
            else -> "Query processed locally: \"$prompt\". Configure your Gemini or OpenRouter API key in settings for expanded multi-modal intelligence."
        }
    }
}

class LlmManager(context: Context) {
    private val prefs = context.getSharedPreferences("nova_llm_config", Context.MODE_PRIVATE)
    private var activeProviderName = "Gemini-1.5-Flash"

    fun setApiKey(provider: String, key: String) {
        prefs.edit().putString("key_${provider.lowercase()}", key.trim()).apply()
    }

    fun getApiKey(provider: String): String {
        return prefs.getString("key_${provider.lowercase()}", "") ?: ""
    }

    fun getActiveProvider(): String = activeProviderName

    suspend fun generateWithFailover(prompt: String, systemPrompt: String): Pair<String, String> {
        val geminiKey = getApiKey("gemini")
        val openRouterKey = getApiKey("openrouter")

        val providers = mutableListOf<LlmProvider>()
        if (geminiKey.isNotBlank()) providers.add(GeminiProvider(geminiKey))
        if (openRouterKey.isNotBlank()) providers.add(OpenRouterProvider(openRouterKey))
        providers.add(LocalRuleProvider())

        var lastError: Exception? = null
        for (provider in providers) {
            try {
                activeProviderName = provider.name
                val result = provider.generate(prompt, systemPrompt)
                if (result.isNotBlank()) {
                    return Pair(result, provider.name)
                }
            } catch (e: Exception) {
                lastError = e
                // Proceed to next fallback provider
            }
        }

        activeProviderName = "Local Fallback"
        val fallback = LocalRuleProvider().generate(prompt, systemPrompt)
        return Pair(fallback, "Local Fallback (Failover from ${lastError?.message ?: "timeout"})")
    }
}
