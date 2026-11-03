package com.nova.ai.ai

import com.nova.ai.MainActivity
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
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    suspend fun ask(prompt: String, systemPrompt: String): String {
        val geminiKey = getApiKey("gemini")
        if (geminiKey.isNotBlank()) {
            try {
                return callGemini(prompt, systemPrompt, geminiKey)
            } catch (e: Exception) {
                println("Gemini failed: ${e.message}, trying fallback...")
            }
        }
        
        return localFallback(prompt)
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
    
    private fun localFallback(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> 
                "Hey! 👋 Main Nova hoon. Bolo — kya karun?"
            lower.contains("time") -> 
                "Abhi time hai: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
            lower.contains("battery") -> 
                "Battery check karne ke liye native bridge available hai."
            lower.contains("who are you") || lower.contains("kya ho") ->
                "I am Nova AI, your on-device personal intelligence assistant with living orb interface."
            else -> 
                "Samajh gaya. Full intelligence ke liye Settings me Gemini API key add karo. Abhi main basic replies de sakta hoon."
        }
    }
    
    private fun getApiKey(provider: String): String {
        val activity = MainActivity.instance ?: return ""
        val prefs = activity.getSharedPreferences("nova_keys", android.content.Context.MODE_PRIVATE)
        return prefs.getString(provider, "") ?: ""
    }
}
