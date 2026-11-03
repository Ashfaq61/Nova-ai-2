package com.example.routines

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.accessibility.NovaAccessibilityService
import org.json.JSONArray
import org.json.JSONObject

data class RoutineStep(
    val action: String,
    val params: JSONObject
)

data class Routine(
    val name: String,
    val triggerPhrase: String,
    val steps: List<RoutineStep>
)

class RoutineEngine(private val context: Context) {

    private val prefs = context.getSharedPreferences("nova_routines", Context.MODE_PRIVATE)

    init {
        bootstrapDefaultRoutines()
    }

    private fun bootstrapDefaultRoutines() {
        if (!prefs.contains("routines_list")) {
            val defaults = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "Good Morning")
                    put("trigger_phrase", "good morning")
                    put("steps", JSONArray().apply {
                        put(JSONObject().apply {
                            put("action", "brightness")
                            put("params", JSONObject().put("value", 85))
                        })
                        put(JSONObject().apply {
                            put("action", "speak")
                            put("params", JSONObject().put("text", "Good morning! Battery is optimal, weather is pleasant, and your schedule is clear."))
                        })
                    })
                })
                put(JSONObject().apply {
                    put("name", "Deep Work")
                    put("trigger_phrase", "deep work")
                    put("steps", JSONArray().apply {
                        put(JSONObject().apply {
                            put("action", "vibrate")
                            put("params", JSONObject().put("duration", 80))
                        })
                        put(JSONObject().apply {
                            put("action", "speak")
                            put("params", JSONObject().put("text", "Focus mode engaged. Notifications silenced, ambient frequencies active."))
                        })
                    })
                })
                put(JSONObject().apply {
                    put("name", "Night Owl")
                    put("trigger_phrase", "night owl")
                    put("steps", JSONArray().apply {
                        put(JSONObject().apply {
                            put("action", "brightness")
                            put("params", JSONObject().put("value", 15))
                        })
                        put(JSONObject().apply {
                            put("action", "speak")
                            put("params", JSONObject().put("text", "Dimming display, activating dark filter, ready for rest."))
                        })
                    })
                })
            }
            prefs.edit().putString("routines_list", defaults.toString()).apply()
        }
    }

    fun listRoutines(): String {
        return prefs.getString("routines_list", "[]") ?: "[]"
    }

    fun saveRoutine(name: String, stepsJson: String): Boolean {
        return try {
            val currentArray = JSONArray(listRoutines())
            val newRoutine = JSONObject().apply {
                put("name", name)
                put("trigger_phrase", name.lowercase())
                put("steps", JSONArray(stepsJson))
            }
            currentArray.put(newRoutine)
            prefs.edit().putString("routines_list", currentArray.toString()).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun runRoutine(name: String, onStepExecuted: ((stepName: String) -> Unit)? = null): Boolean {
        val jsonStr = listRoutines()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val r = array.getJSONObject(i)
            if (r.getString("name").equals(name, ignoreCase = true)) {
                val steps = r.getJSONArray("steps")
                executeStepsSequentially(steps, 0, onStepExecuted)
                return true
            }
        }
        return false
    }

    private fun executeStepsSequentially(steps: JSONArray, index: Int, onStepExecuted: ((String) -> Unit)?) {
        if (index >= steps.length()) return
        val step = steps.getJSONObject(index)
        val action = step.getString("action")
        val params = step.optJSONObject("params") ?: JSONObject()

        onStepExecuted?.invoke(action)

        when (action) {
            "tap" -> {
                val x = params.optDouble("x", 200.0).toFloat()
                val y = params.optDouble("y", 400.0).toFloat()
                NovaAccessibilityService.instance?.dispatchTap(x, y)
            }
            "home" -> {
                NovaAccessibilityService.instance?.performGlobal(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            }
            "back" -> {
                NovaAccessibilityService.instance?.performGlobal(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            executeStepsSequentially(steps, index + 1, onStepExecuted)
        }, 600)
    }
}
