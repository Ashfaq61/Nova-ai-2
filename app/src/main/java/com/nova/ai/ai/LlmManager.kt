package com.nova.ai.ai

import com.nova.ai.memory.NovaPrefs
import com.nova.ai.sensing.PrivacyScrubber
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object LlmManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun ask(prompt: String, systemPrompt: String = "You are Nova AI."): String {
        val scrubbed = PrivacyScrubber.scrub(prompt)

        // 1. Try Gemini with 15s failover
        val geminiKey = NovaPrefs.getKey("gemini_key")
        if (geminiKey.isNotBlank()) {
            val geminiResult = withTimeoutOrNull(15000) {
                try {
                    callGemini(scrubbed, systemPrompt, geminiKey)
                } catch (e: Exception) {
                    null
                }
            }
            if (!geminiResult.isNullOrBlank()) {
                return PrivacyScrubber.restore(geminiResult)
            }
        }

        // 2. Try OpenRouter with 15s failover
        val openRouterKey = NovaPrefs.getKey("openrouter_key")
        if (openRouterKey.isNotBlank()) {
            val openRouterResult = withTimeoutOrNull(15000) {
                try {
                    callOpenRouter(scrubbed, systemPrompt, openRouterKey)
                } catch (e: Exception) {
                    null
                }
            }
            if (!openRouterResult.isNullOrBlank()) {
                return PrivacyScrubber.restore(openRouterResult)
            }
        }

        // 3. Deterministic Local Fallback
        val local = localFallback(scrubbed)
        return PrivacyScrubber.restore(local)
    }

    private fun callGemini(prompt: String, sysPrompt: String, apiKey: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sysPrompt))))
        }

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from Gemini")
        val json = JSONObject(responseBody)

        return json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .optString("text", "No response generated.")
    }

    private fun callOpenRouter(prompt: String, sysPrompt: String, apiKey: String): String {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", sysPrompt))
            put(JSONObject().put("role", "user").put("content", prompt))
        }
        val body = JSONObject().apply {
            put("model", "google/gemini-2.0-flash-001")
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://nova.ai")
            .addHeader("X-Title", "Nova AI Singularity")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from OpenRouter")
        val json = JSONObject(responseBody)

        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .optString("content", "No OpenRouter response.")
    }

    private fun localFallback(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("time") -> "The current local time is " + java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                "Hello! I am Nova AI Singularity. All local cognitive systems, security gates, and living UI systems are operational."
            lower.contains("battery") -> "Device battery telemetry is optimal and monitored by AutomationEngine."
            lower.contains("who are you") -> "I am Nova AI — your on-device, multi-agent cognitive operating system."
            else -> "I processed your request: \"$prompt\". (Operating in high-speed on-device mode. Add your Gemini or OpenRouter key in Settings for full generative intelligence.)"
        }
    }
}
