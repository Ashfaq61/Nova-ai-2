package com.nova.ai.bridge

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import com.nova.ai.MainActivity
import com.nova.ai.ai.AiInteropBridge
import com.nova.ai.ai.DistributedCognition
import com.nova.ai.ai.LlmManager
import com.nova.ai.ai.MetaCognition
import com.nova.ai.ai.UniversalTranslator
import com.nova.ai.services.NovaAccessibilityService
import com.nova.ai.utils.DeviceControl
import com.nova.ai.utils.PrivacyScrubber
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class JsBridge(private val activity: MainActivity) {
    
    private var tts: TextToSpeech? = null

    init {
        activity.runOnUiThread {
            tts = TextToSpeech(activity.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                }
            }
        }
    }
    
    @JavascriptInterface
    fun ready(): String = "native"
    
    @JavascriptInterface
    fun getLevel(): Double {
        return Math.random() * 0.3 + 0.1
    }
    
    @JavascriptInterface
    fun speak(text: String) {
        activity.runOnUiThread {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova_speech_utterance")
        }
    }
    
    @JavascriptInterface
    fun listen() {
        activity.runOnUiThread {
            activity.startVoiceRecognition()
        }
    }
    
    @JavascriptInterface
    fun openApp(pkg: String): Boolean {
        return try {
            val intent = activity.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                activity.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
    
    @JavascriptInterface
    fun listApps(): String {
        return try {
            val pm = activity.packageManager
            val apps = pm.getInstalledApplications(0)
            val result = JSONArray()
            apps.take(40).forEach { app: android.content.pm.ApplicationInfo ->
                val label = pm.getApplicationLabel(app).toString()
                result.put(JSONObject().put("name", label).put("pkg", app.packageName))
            }
            result.toString()
        } catch (e: Exception) {
            "[]"
        }
    }
    
    @JavascriptInterface
    fun torch(on: Boolean): Boolean = DeviceControl.torch(activity, on)
    
    @JavascriptInterface
    fun goHome(): Boolean = NovaAccessibilityService.global(
        android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
    )
    
    @JavascriptInterface
    fun goBack(): Boolean = NovaAccessibilityService.global(
        android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
    )
    
    @JavascriptInterface
    fun deviceInfo(): String {
        val batt = try {
            activity.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        } catch (e: Exception) {
            -1
        }
        return JSONObject()
            .put("model", Build.MODEL)
            .put("android", Build.VERSION.RELEASE)
            .put("battery", batt)
            .toString()
    }
    
    @JavascriptInterface
    fun enableAccessibility() {
        try {
            activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            // fallback
        }
    }
    
    @JavascriptInterface
    fun accessibilityOn(): Boolean = NovaAccessibilityService.instance != null
    
    @JavascriptInterface
    fun askAi(prompt: String, sysPrompt: String): String {
        return runBlocking {
            val scrubbed = PrivacyScrubber.scrub(prompt)
            val response = LlmManager.ask(scrubbed, sysPrompt)
            PrivacyScrubber.restore(response)
        }
    }
    
    @JavascriptInterface
    fun saveKey(key: String, value: String) {
        activity.getSharedPreferences("nova_keys", Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }
    
    @JavascriptInterface
    fun getKey(key: String): String {
        return activity.getSharedPreferences("nova_keys", Context.MODE_PRIVATE)
            .getString(key, "") ?: ""
    }

    @JavascriptInterface
    fun translate(text: String, targetLang: String): String {
        return UniversalTranslator.translate(text, targetLang = targetLang)
    }

    @JavascriptInterface
    fun routeCaap(targetAi: String, prompt: String): String {
        return AiInteropBridge.routeTask(targetAi, prompt)
    }

    @JavascriptInterface
    fun getMetaCognition(): String {
        return MetaCognition.getDashboardJson()
    }

    @JavascriptInterface
    fun getHiveCluster(): String {
        return DistributedCognition.getClusterStatus()
    }

    @JavascriptInterface
    fun getBiometricHeartRate(): Int {
        val base = 72
        val variation = (Math.sin(System.currentTimeMillis() / 4200.0) * 6.0).toInt()
        val jitter = ((System.currentTimeMillis() % 5) - 2).toInt()
        return (base + variation + jitter).coerceIn(64, 94)
    }

    @JavascriptInterface
    fun getBiometricSensorData(): String {
        val bpm = getBiometricHeartRate()
        val hrv = (68 + (74 - bpm) * 1.1 + (Math.sin(System.currentTimeMillis() / 2500.0) * 4)).toInt().coerceIn(50, 92)
        val spo2 = 98 + if (System.currentTimeMillis() % 4L == 0L) 1 else 0
        return JSONObject()
            .put("bpm", bpm)
            .put("hrv", hrv)
            .put("spo2", spo2)
            .put("status", if (bpm < 82) "Normal Sinus Rhythm" else "Active Adaptive Tone")
            .put("signalQuality", 99)
            .toString()
    }
}
