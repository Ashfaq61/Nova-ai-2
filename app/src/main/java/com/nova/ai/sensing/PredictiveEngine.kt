package com.nova.ai.sensing

import org.json.JSONObject

object PredictiveEngine {

    // Markov transition counts: ContextKey -> (Action -> Count)
    private val transitions = mutableMapOf<String, MutableMap<String, Int>>()

    init {
        // Pre-seed common contextual habits
        recordTransition("morning_low_battery", "Enable Battery Saver")
        recordTransition("morning_low_battery", "Enable Battery Saver")
        recordTransition("morning_low_battery", "Enable Battery Saver")
        recordTransition("night_plugged", "Mute Notifications & Set Alarm")
        recordTransition("night_plugged", "Mute Notifications & Set Alarm")
        recordTransition("night_plugged", "Mute Notifications & Set Alarm")
        recordTransition("work_wifi", "Open Calendar & Slack")
        recordTransition("work_wifi", "Open Calendar & Slack")
        recordTransition("work_wifi", "Open Calendar & Slack")
        recordTransition("work_wifi", "Open Calendar & Slack")
    }

    fun recordTransition(contextState: String, action: String) {
        val actionMap = transitions.getOrPut(contextState) { mutableMapOf() }
        actionMap[action] = (actionMap[action] ?: 0) + 1
    }

    fun predictNextAction(contextState: String): PredictionResult? {
        val actionMap = transitions[contextState] ?: return null
        val totalOccurrences = actionMap.values.sum()
        if (totalOccurrences == 0) return null

        val bestAction = actionMap.maxByOrNull { it.value } ?: return null
        val probability = bestAction.value.toFloat() / totalOccurrences

        // Only recommend if Markov confidence exceeds 75% threshold
        return if (probability >= 0.75f) {
            PredictionResult(
                action = bestAction.key,
                confidence = probability,
                context = contextState,
                isRecommended = true
            )
        } else {
            PredictionResult(
                action = bestAction.key,
                confidence = probability,
                context = contextState,
                isRecommended = false
            )
        }
    }

    fun getSuggestionsJson(contextState: String): String {
        val p = predictNextAction(contextState)
        return if (p != null) {
            JSONObject()
                .put("action", p.action)
                .put("confidence", (p.confidence * 100).toInt())
                .put("context", p.context)
                .put("recommended", p.isRecommended)
                .toString()
        } else {
            JSONObject().put("status", "No pattern >75% confidence yet").toString()
        }
    }
}

data class PredictionResult(
    val action: String,
    val confidence: Float,
    val context: String,
    val isRecommended: Boolean
)
