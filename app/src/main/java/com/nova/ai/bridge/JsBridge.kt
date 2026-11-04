package com.nova.ai.bridge

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import com.nova.ai.MainActivity
import com.nova.ai.agent.AutomationEngine
import com.nova.ai.agent.UiAgent
import com.nova.ai.agent.WorldModel
import com.nova.ai.ai.AiInteropBridge
import com.nova.ai.ai.DistributedCognition
import com.nova.ai.ai.LlmManager
import com.nova.ai.ai.MetaCognition
import com.nova.ai.ai.UniversalTranslator
import com.nova.ai.life.DeviceControl
import com.nova.ai.life.IdentityCore
import com.nova.ai.life.PcLinkServer
import com.nova.ai.life.RoutineEngine
import com.nova.ai.life.UpdateChecker
import com.nova.ai.memory.KnowledgeGraph
import com.nova.ai.memory.MemoryDb
import com.nova.ai.memory.NovaPrefs
import com.nova.ai.sensing.BioSignal
import com.nova.ai.sensing.EmotionEngine
import com.nova.ai.sensing.PredictiveEngine
import com.nova.ai.sensing.PrivacyScrubber
import com.nova.ai.services.MeshNetworkService
import com.nova.ai.services.NovaAccessibilityService
import com.nova.ai.services.NovaNotificationListener
import com.nova.ai.services.ScreenCaptureService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class JsBridge(private val activity: MainActivity) {

    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    init {
        activity.runOnUiThread {
            // Rule 1: NEVER reference 'tts' inside this listener lambda!
            tts = TextToSpeech(activity.applicationContext, object : TextToSpeech.OnInitListener {
                override fun onInit(status: Int) {
                    if (status == TextToSpeech.SUCCESS) {
                        isTtsReady = true
                    }
                }
            })
        }
    }

    @JavascriptInterface
    fun ready(): String = "native_phase10_singularity"

    // Audio-energy for Living Orb Canvas reactivity
    @JavascriptInterface
    fun getLevel(): Double {
        return Math.random() * 0.35 + 0.1
    }

    @JavascriptInterface
    fun speak(text: String) {
        activity.runOnUiThread {
            tts?.let { engine ->
                try {
                    engine.language = Locale.US
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nova_voice_out")
                } catch (e: Exception) {}
            }
        }
    }

    @JavascriptInterface
    fun startListening() {
        activity.runOnUiThread {
            activity.startVoiceRecognition()
        }
    }

    @JavascriptInterface
    fun stopListening() {
        activity.runOnUiThread {
            activity.stopVoiceRecognition()
        }
    }

    // --- Device Controls & Accessibility ---
    @JavascriptInterface
    fun tap(x: Float, y: Float): Boolean = NovaAccessibilityService.tap(x, y)

    @JavascriptInterface
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): Boolean =
        NovaAccessibilityService.swipe(startX, startY, endX, endY, durationMs)

    @JavascriptInterface
    fun typeText(text: String): Boolean = NovaAccessibilityService.typeText(text)

    @JavascriptInterface
    fun readScreen(): String = NovaAccessibilityService.readScreen()

    @JavascriptInterface
    fun goHome(): Boolean = NovaAccessibilityService.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)

    @JavascriptInterface
    fun goBack(): Boolean = NovaAccessibilityService.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)

    @JavascriptInterface
    fun openApp(pkg: String): Boolean {
        return try {
            val intent = activity.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
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
            apps.take(45).forEach { app: android.content.pm.ApplicationInfo ->
                val label = pm.getApplicationLabel(app).toString()
                result.put(JSONObject().put("name", label).put("pkg", app.packageName))
            }
            result.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    @JavascriptInterface
    fun toggleTorch(): Boolean = DeviceControl.toggleTorch(activity)

    @JavascriptInterface
    fun torch(on: Boolean): Boolean = DeviceControl.torch(activity, on)

    @JavascriptInterface
    fun vibrate(milliseconds: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(milliseconds)
            }
        } catch (e: Exception) {}
    }

    @JavascriptInterface
    fun setVolume(percent: Int): Boolean = DeviceControl.setVolume(activity, percent = percent)

    @JavascriptInterface
    fun setBrightness(percent: Int): Boolean = DeviceControl.setBrightness(activity, percent = percent)

    @JavascriptInterface
    fun setSilent(on: Boolean): Boolean = DeviceControl.setSilentMode(activity, on)

    @JavascriptInterface
    fun lockScreen(): Boolean = DeviceControl.lockScreen(activity)

    @JavascriptInterface
    fun accessibilityOn(): Boolean = NovaAccessibilityService.instance != null

    @JavascriptInterface
    fun openAccessibilitySettings() {
        try {
            activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {}
    }

    @JavascriptInterface
    fun deviceInfo(): String {
        val batt = try {
            activity.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        } catch (e: Exception) { -1 }

        return JSONObject()
            .put("model", Build.MODEL)
            .put("android", Build.VERSION.RELEASE)
            .put("battery", batt)
            .put("accessibility", NovaAccessibilityService.instance != null)
            .put("meshActive", MeshNetworkService.isMeshActive)
            .put("pcLinkActive", PcLinkServer.isServerRunning)
            .toString()
    }

    // --- LLM & AI Orchestration ---
    @JavascriptInterface
    fun askAi(prompt: String, sysPrompt: String = ""): String {
        return runBlocking {
            val system = if (sysPrompt.isBlank()) "You are Nova AI Singularity. Be helpful, concise, and futuristic." else sysPrompt
            val scrubbed = PrivacyScrubber.scrub(prompt)
            val res = LlmManager.ask(scrubbed, system)
            PrivacyScrubber.restore(res)
        }
    }

    @JavascriptInterface
    fun saveKey(key: String, value: String) {
        NovaPrefs.setKey(key, value)
    }

    @JavascriptInterface
    fun getKey(key: String): String {
        return NovaPrefs.getKey(key, "")
    }

    @JavascriptInterface
    fun saveExternalKeys(jsonStr: String) {
        try {
            val obj = JSONObject(jsonStr)
            if (obj.has("openai")) NovaPrefs.setKey("openai_key", obj.getString("openai"))
            if (obj.has("anthropic")) NovaPrefs.setKey("anthropic_key", obj.getString("anthropic"))
            if (obj.has("gemini")) NovaPrefs.setKey("gemini_key", obj.getString("gemini"))
            if (obj.has("openrouter")) NovaPrefs.setKey("openrouter_key", obj.getString("openrouter"))
        } catch (e: Exception) {}
    }

    @JavascriptInterface
    fun getExternalKey(provider: String): String {
        return when (provider.lowercase()) {
            "openai", "chatgpt" -> NovaPrefs.getKey("openai_key")
            "anthropic", "claude" -> NovaPrefs.getKey("anthropic_key")
            "gemini" -> NovaPrefs.getKey("gemini_key")
            "openrouter" -> NovaPrefs.getKey("openrouter_key")
            else -> ""
        }
    }

    @JavascriptInterface
    fun routeCaap(targetAi: String, prompt: String): String {
        return AiInteropBridge.routeTask(targetAi, prompt)
    }

    @JavascriptInterface
    fun runConsensusCheck(query: String): String {
        return DistributedCognition.evaluateConsensus(query)
    }

    @JavascriptInterface
    fun getMetaCognition(): String = MetaCognition.getDashboardJson()

    @JavascriptInterface
    fun getRecentThoughts(): String = MetaCognition.getRecentThoughtsJson()

    @JavascriptInterface
    fun getEthicsReport(): String = IdentityCore.getReportJson()

    // --- UI Agent & WorldModel ---
    @JavascriptInterface
    fun runUiAgent(goal: String): String {
        return runBlocking {
            val res = UiAgent.executeGoal(goal)
            JSONObject().put("success", res.success).put("message", res.message).toString()
        }
    }

    @JavascriptInterface
    fun dryRunUiAgent(goal: String): String {
        return WorldModel.getDryRunJson(goal)
    }

    // --- Memory & KnowledgeGraph ---
    @JavascriptInterface
    fun remember(content: String): Boolean {
        val id = MemoryDb.getInstance(activity).remember(content)
        KnowledgeGraph.getInstance(activity).addEvent(content, "User Voice Input", "User")
        return id > 0
    }

    @JavascriptInterface
    fun recall(query: String): String {
        return MemoryDb.getInstance(activity).recallJson(query)
    }

    @JavascriptInterface
    fun queryKnowledge(question: String): String {
        return KnowledgeGraph.getInstance(activity).answerNaturalLanguage(question)
    }

    // --- Sensing & Biometrics ---
    @JavascriptInterface
    fun getBiometricHeartRate(): Int {
        return BioSignal.getLatestReading().bpm
    }

    @JavascriptInterface
    fun getBiometricSensorData(): String {
        return BioSignal.toJson()
    }

    @JavascriptInterface
    fun getEmotion(): String {
        return EmotionEngine.toJson()
    }

    @JavascriptInterface
    fun predictNextAction(contextState: String): String {
        return PredictiveEngine.getSuggestionsJson(contextState)
    }

    // --- Routines & Automation ---
    @JavascriptInterface
    fun runRoutine(routineName: String): Boolean {
        return RoutineEngine.runRoutine(activity, routineName)
    }

    @JavascriptInterface
    fun listRoutines(): String {
        return RoutineEngine.getRoutinesListJson()
    }

    @JavascriptInterface
    fun addAutomationRule(triggerType: String, triggerValue: String, action: String): String {
        return AutomationEngine.addRule(triggerType, triggerValue, action)
    }

    @JavascriptInterface
    fun listAutomationRules(): String {
        return AutomationEngine.getRulesJson()
    }

    // --- Translation & Earbud Loop ---
    @JavascriptInterface
    fun translate(text: String, targetLang: String): String {
        return UniversalTranslator.translate(text, targetLang = targetLang)
    }

    @JavascriptInterface
    fun startTranslator(sourceLang: String, targetLang: String) {
        UniversalTranslator.startEarbudLoop(activity, targetLang)
    }

    @JavascriptInterface
    fun stopTranslator() {
        UniversalTranslator.stopEarbudLoop()
    }

    // --- Swarm & PC Link ---
    @JavascriptInterface
    fun getMeshStatus(): String {
        val arr = JSONArray()
        MeshNetworkService.connectedNodes.forEach { arr.put(it) }
        return JSONObject()
            .put("active", MeshNetworkService.isMeshActive)
            .put("port", 8766)
            .put("connectedNodes", arr)
            .toString()
    }

    @JavascriptInterface
    fun togglePcLink(enable: Boolean): Boolean {
        if (enable) PcLinkServer.start(activity, 8765)
        else PcLinkServer.stop()
        return PcLinkServer.isServerRunning
    }

    @JavascriptInterface
    fun getPcLinkStatus(): String {
        return JSONObject()
            .put("running", PcLinkServer.isServerRunning)
            .put("port", 8765)
            .put("url", "http://localhost:8765")
            .toString()
    }

    // --- Auto Update ---
    @JavascriptInterface
    fun checkForUpdates(): String {
        return runBlocking {
            val info = UpdateChecker.checkForUpdates(activity)
            if (info != null) {
                JSONObject()
                    .put("hasUpdate", true)
                    .put("version", info.version)
                    .put("changelog", info.changelog)
                    .put("apkUrl", info.apkUrl)
                    .toString()
            } else {
                JSONObject().put("hasUpdate", false).put("message", "Nova AI is on the latest Singularity build.").toString()
            }
        }
    }
}
