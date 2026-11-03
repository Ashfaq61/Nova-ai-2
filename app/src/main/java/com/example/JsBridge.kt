package com.example

import android.webkit.JavascriptInterface
import com.example.accessibility.NovaAccessibilityService
import com.example.ar.ArOverlayEngine
import com.example.biometrics.BioFusion
import com.example.distributed.DistributedCognition
import com.example.emotion.EmotionEngine
import com.example.identity.IdentityCore
import com.example.interop.AiInteropBridge
import com.example.llm.LlmManager
import com.example.memory.KnowledgeGraph
import com.example.mesh.MeshNetwork
import com.example.metacognition.MetaCognition
import com.example.pclink.PcLinkServer
import com.example.predictive.PredictiveEngine
import com.example.privacy.PrivacyScrubber
import com.example.routines.RoutineEngine
import com.example.services.DreamWorker
import com.example.translation.UniversalTranslator
import com.example.update.UpdateChecker
import com.example.worldmodel.WorldModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class JsBridge(private val activity: MainActivity) {

    private val routineEngine = RoutineEngine(activity)
    private val pcLinkServer = PcLinkServer(activity)
    private val llmManager = LlmManager(activity)
    private val updateChecker = UpdateChecker(activity)
    private val arEngine = ArOverlayEngine()
    private val emotionEngine = EmotionEngine()
    private val predictiveEngine = PredictiveEngine()
    private val meshNetwork = MeshNetwork()
    private val bioFusion = BioFusion()
    private val knowledgeGraph = KnowledgeGraph(activity)
    private val identityCore = IdentityCore()
    private val worldModel = WorldModel()
    private val privacyScrubber = PrivacyScrubber()

    init {
        AiInteropBridge.init(activity)
    }

    // --- Core Device & Speech Controls ---
    @JavascriptInterface
    fun startListening() {
        activity.startSpeechRecognition()
    }

    @JavascriptInterface
    fun stopListening() {
        activity.stopSpeechRecognition()
    }

    @JavascriptInterface
    fun speak(text: String) {
        activity.speakOut(text)
    }

    @JavascriptInterface
    fun toggleTorch(): Boolean {
        return activity.toggleFlashlight()
    }

    @JavascriptInterface
    fun getBatteryLevel(): Int {
        return activity.getBatteryPercentage()
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        activity.triggerHaptic(durationMs)
    }

    // --- Accessibility Automation ---
    @JavascriptInterface
    fun dispatchTap(x: Float, y: Float): Boolean {
        return NovaAccessibilityService.instance?.dispatchTap(x, y) ?: false
    }

    @JavascriptInterface
    fun dispatchSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): Boolean {
        return NovaAccessibilityService.instance?.dispatchSwipe(x1, y1, x2, y2, durationMs) ?: false
    }

    @JavascriptInterface
    fun dispatchText(text: String): Boolean {
        return NovaAccessibilityService.instance?.dispatchText(text) ?: false
    }

    @JavascriptInterface
    fun performGlobalAction(actionId: Int): Boolean {
        return NovaAccessibilityService.instance?.performGlobal(actionId) ?: false
    }

    // --- Module 1: Routine Engine & Macros ---
    @JavascriptInterface
    fun saveRoutine(name: String, stepsJson: String): Boolean {
        return routineEngine.saveRoutine(name, stepsJson)
    }

    @JavascriptInterface
    fun runRoutine(name: String): Boolean {
        return routineEngine.runRoutine(name) { step ->
            activity.runOnUiThread {
                activity.evaluateJs("if (window.onRoutineStep) window.onRoutineStep('$name', '$step');")
            }
        }
    }

    @JavascriptInterface
    fun listRoutines(): String {
        return routineEngine.listRoutines()
    }

    // --- Module 2: PC Link ---
    @JavascriptInterface
    fun startPcLink(): Boolean {
        return pcLinkServer.start()
    }

    @JavascriptInterface
    fun stopPcLink() {
        pcLinkServer.stop()
    }

    @JavascriptInterface
    fun getLocalIp(): String {
        return pcLinkServer.getLocalIp()
    }

    @JavascriptInterface
    fun getPcLinkStatus(): String {
        return JSONObject().apply {
            put("ip", pcLinkServer.getLocalIp())
            put("port", PcLinkServer.PORT)
            put("pairingCode", pcLinkServer.pairingCode)
        }.toString()
    }

    // --- Module 4: Multi-LLM Failover ---
    @JavascriptInterface
    fun setApiKey(provider: String, key: String) {
        llmManager.setApiKey(provider, key)
    }

    @JavascriptInterface
    fun getApiKey(provider: String): String {
        return llmManager.getApiKey(provider)
    }

    @JavascriptInterface
    fun getActiveProvider(): String {
        return llmManager.getActiveProvider()
    }

    @JavascriptInterface
    fun generateLlm(prompt: String, systemPrompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            MetaCognition.think("User_Input", prompt.take(50))
            val (scrubbedPrompt, mapping) = privacyScrubber.scrub(prompt)
            val (response, provider) = llmManager.generateWithFailover(scrubbedPrompt, systemPrompt)
            val restoredResponse = privacyScrubber.unscrub(response, mapping)

            MetaCognition.think("LLM_Generated", "Provider=$provider -> ${restoredResponse.take(40)}...")

            activity.runOnUiThread {
                val safeText = JSONObject.quote(restoredResponse)
                val safeProvider = JSONObject.quote(provider)
                activity.evaluateJs("if (window.onLlmResponse) window.onLlmResponse($safeText, $safeProvider);")
            }
        }
    }

    // --- Module 5: Auto-Update ---
    @JavascriptInterface
    fun getCurrentVersion(): String = updateChecker.getCurrentVersion()

    @JavascriptInterface
    fun checkForUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = updateChecker.checkForUpdates()
            activity.runOnUiThread {
                val safeResult = JSONObject.quote(result)
                activity.evaluateJs("if (window.onUpdateCheckResult) window.onUpdateCheckResult($safeResult);")
            }
        }
    }

    // --- Module 6: AR Vision ---
    @JavascriptInterface
    fun toggleAR(enabled: Boolean): Boolean = arEngine.toggleAR(enabled)

    @JavascriptInterface
    fun searchObject(query: String): String = arEngine.searchObject(query)

    @JavascriptInterface
    fun getDetectedObjects(): String = arEngine.getDetectedObjects()

    // --- Module 7: Emotion & Voice ---
    @JavascriptInterface
    fun enrollVoice(sampleMeta: String): Boolean = emotionEngine.enrollVoice(sampleMeta)

    @JavascriptInterface
    fun setVoiceMode(mode: String) = emotionEngine.setVoiceMode(mode)

    @JavascriptInterface
    fun getEmotionTTSParams(mood: String): String = emotionEngine.getEmotionTTSParams(mood)

    // --- Module 8: Predictive Engine ---
    @JavascriptInterface
    fun recordAction(action: String) = predictiveEngine.recordAction("user_action", action)

    @JavascriptInterface
    fun getPrediction(): String = predictiveEngine.getPrediction()

    @JavascriptInterface
    fun suppressPattern(patternId: String) = predictiveEngine.suppressPattern(patternId)

    // --- Module 9: Mesh Network ---
    @JavascriptInterface
    fun startMesh(): Boolean = meshNetwork.startMesh()

    @JavascriptInterface
    fun pairWith(code: String): String = meshNetwork.pairWith(code)

    @JavascriptInterface
    fun sendToPeer(peerId: String, payloadJson: String): String = meshNetwork.sendToPeer(peerId, payloadJson)

    @JavascriptInterface
    fun getPeers(): String = meshNetwork.getPeers()

    // --- Module 10: Biometric Mood Fusion ---
    @JavascriptInterface
    fun registerBioSensors(): Boolean = bioFusion.registerBioSensors()

    @JavascriptInterface
    fun getHeartRate(): Int = bioFusion.getHeartRate()

    @JavascriptInterface
    fun getStressIndex(): Float = bioFusion.getStressIndex()

    @JavascriptInterface
    fun getMoodVector(): String = bioFusion.getMoodVector()

    // --- Module 11: Self-Evolving & Dream Cycle ---
    @JavascriptInterface
    fun triggerDreamCycle(): String {
        DreamWorker.executeConsolidation(activity) { summary ->
            activity.runOnUiThread {
                val safeSummary = JSONObject.quote(summary)
                activity.evaluateJs("if (window.onDreamCycleComplete) window.onDreamCycleComplete($safeSummary);")
            }
        }
        return DreamWorker.getLastSummary(activity)
    }

    @JavascriptInterface
    fun getDiagnostics(): String = DreamWorker.getLastSummary(activity)

    // --- Module 12: Privacy & Federated ---
    @JavascriptInterface
    fun getPrivacyStats(): String = privacyScrubber.getPrivacyStats()

    @JavascriptInterface
    fun toggleFederatedLearning(enabled: Boolean): Boolean = privacyScrubber.toggleFederatedLearning(enabled)

    @JavascriptInterface
    fun exportAuditLog(): String = privacyScrubber.exportAuditLog()

    @JavascriptInterface
    fun scrubText(text: String): String = privacyScrubber.scrub(text).first

    // --- Memory: Knowledge Graph ---
    @JavascriptInterface
    fun queryRecentFacts(limit: Int): String = knowledgeGraph.queryRecentFacts(limit)

    @JavascriptInterface
    fun saveFact(entity: String, relation: String, target: String): Boolean {
        return knowledgeGraph.saveFact(entity, relation, target) > 0
    }

    // --- Safety & World Model ---
    @JavascriptInterface
    fun evaluateSafety(action: String, paramsJson: String): String = identityCore.evaluateSafetyGate(action, paramsJson)

    @JavascriptInterface
    fun simulatePreMortem(action: String, paramsJson: String): String = worldModel.simulatePreMortem(action, paramsJson)

    // ═════════════════════════════════════════════════════════════════════════
    // PHASE 10: THE OMNICONNECTED SINGULARITY BRIDGE METHODS
    // ═════════════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun enableAiInterop(enabled: Boolean) {
        AiInteropBridge.isEnabled = enabled
        MetaCognition.think("Bridge", "Cross-AI Interoperability state: $enabled")
    }

    @JavascriptInterface
    fun saveExternalKeys(keysJson: String) {
        val keys = JSONObject(keysJson)
        val openai = keys.optString("openai", "")
        val anthropic = keys.optString("anthropic", "")
        AiInteropBridge.saveKeys(activity, openai, anthropic)
    }

    @JavascriptInterface
    fun getExternalKey(provider: String): String {
        return AiInteropBridge.getKey(provider)
    }

    @JavascriptInterface
    fun executeAiDag(dagJson: String): String {
        return runBlocking {
            val dag = AiInteropBridge.parseDagFromJson(dagJson)
            val results = AiInteropBridge.executeDag(dag) { nodeId, status ->
                activity.runOnUiThread {
                    activity.evaluateJs("if (window.onDagNodeProgress) window.onDagNodeProgress('$nodeId', '$status');")
                }
            }
            JSONObject(results).toString()
        }
    }

    @JavascriptInterface
    fun getRecentThoughts(): String {
        val list = MetaCognition.getRecentThoughts(15)
        return JSONArray(list).toString()
    }

    @JavascriptInterface
    fun assessCapability(request: String): String {
        val assessment = MetaCognition.assessCapability(request)
        return JSONObject().apply {
            put("can_fulfill", assessment.canFulfill)
            put("skill", assessment.requiredSkill)
            put("statement", assessment.honestyStatement)
            put("alternative", assessment.alternativeSuggestion)
        }.toString()
    }

    @JavascriptInterface
    fun startTranslator(source: String, target: String) {
        UniversalTranslator.startEarbudMode(source, target)
    }

    @JavascriptInterface
    fun stopTranslator() {
        UniversalTranslator.stopEarbudMode()
    }

    @JavascriptInterface
    fun translateText(text: String, from: String, to: String): String {
        return runBlocking {
            UniversalTranslator.translateText(text, from, to)
        }
    }

    @JavascriptInterface
    fun runConsensusCheck(query: String): String {
        return runBlocking {
            val result = DistributedCognition.consensusCheck(query)
            DistributedCognition.consensusResultToJson(result)
        }
    }

    @JavascriptInterface
    fun runParallelResearch(query: String, anglesJson: String): String {
        return runBlocking {
            val arr = JSONArray(anglesJson)
            val angles = mutableListOf<String>()
            for (i in 0 until arr.length()) angles.add(arr.getString(i))
            val report = DistributedCognition.parallelResearch(query, angles)
            DistributedCognition.researchReportToJson(report)
        }
    }

    @JavascriptInterface
    fun getMetaCognitionDashboardData(): String {
        return MetaCognition.getDashboardMetrics()
    }
}
