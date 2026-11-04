package com.nova.ai.life

import android.content.Context
import android.content.Intent
import com.nova.ai.services.NovaAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object RoutineEngine {

    private val routines = mutableMapOf<String, JSONArray>()
    private val recordedSteps = mutableListOf<JSONObject>()
    private var isRecording = false

    init {
        // Defaults: Morning Routine
        val morningSteps = JSONArray().apply {
            put(JSONObject().put("action", "speak").put("text", "Good morning! Setting up your workspace and briefing today's agenda."))
            put(JSONObject().put("action", "volume").put("percent", 65))
            put(JSONObject().put("action", "torch").put("state", false))
        }
        routines["morning"] = morningSteps

        // Defaults: Bedtime Routine
        val bedtimeSteps = JSONArray().apply {
            put(JSONObject().put("action", "speak").put("text", "Good night. Engaging low brightness and quiet mode."))
            put(JSONObject().put("action", "silent").put("state", true))
            put(JSONObject().put("action", "brightness").put("percent", 10))
        }
        routines["bedtime"] = bedtimeSteps

        // Defaults: Battery Saver Routine
        val batterySteps = JSONArray().apply {
            put(JSONObject().put("action", "brightness").put("percent", 15))
            put(JSONObject().put("action", "speak").put("text", "Low battery detected. Conserving power."))
        }
        routines["battery_saver"] = batterySteps
    }

    fun startRecording() {
        isRecording = true
        recordedSteps.clear()
    }

    fun recordAction(actionType: String, params: JSONObject) {
        if (!isRecording) return
        val step = JSONObject().put("action", actionType)
        params.keys().forEach { k -> step.put(k, params.get(k)) }
        recordedSteps.add(step)
    }

    fun stopRecording(routineName: String): Boolean {
        isRecording = false
        if (recordedSteps.isEmpty()) return false
        val arr = JSONArray()
        recordedSteps.forEach { arr.put(it) }
        routines[routineName.lowercase().trim()] = arr
        return true
    }

    fun runRoutine(context: Context, routineName: String): Boolean {
        val steps = routines[routineName.lowercase().trim()] ?: return false
        CoroutineScope(Dispatchers.Main).launch {
            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)
                executeStep(context, step)
                delay(700)
            }
        }
        return true
    }

    private fun executeStep(context: Context, step: JSONObject) {
        val action = step.optString("action")
        when (action) {
            "torch" -> DeviceControl.torch(context, step.optBoolean("state", true))
            "volume" -> DeviceControl.setVolume(context, percent = step.optInt("percent", 50))
            "brightness" -> DeviceControl.setBrightness(context, percent = step.optInt("percent", 50))
            "silent" -> DeviceControl.setSilentMode(context, step.optBoolean("state", true))
            "tap" -> {
                val x = step.optDouble("x", 500.0).toFloat()
                val y = step.optDouble("y", 800.0).toFloat()
                NovaAccessibilityService.tap(x, y)
            }
            "swipe" -> {
                val sx = step.optDouble("startX", 500.0).toFloat()
                val sy = step.optDouble("startY", 1200.0).toFloat()
                val ex = step.optDouble("endX", 500.0).toFloat()
                val ey = step.optDouble("endY", 400.0).toFloat()
                NovaAccessibilityService.swipe(sx, sy, ex, ey)
            }
            "launch" -> {
                val pkg = step.optString("pkg")
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) context.startActivity(intent)
            }
        }
    }

    fun getRoutinesListJson(): String {
        val arr = JSONArray()
        routines.keys.forEach { arr.put(it) }
        return arr.toString()
    }
}
