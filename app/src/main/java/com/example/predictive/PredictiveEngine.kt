package com.example.predictive

import org.json.JSONObject
import java.util.Calendar

data class PatternRecord(
    val contextKey: String,
    val predictedAction: String,
    val probability: Float,
    var suppressedUntilMs: Long = 0L
)

class PredictiveEngine {

    private val patterns = mutableMapOf<String, PatternRecord>(
        "morning_home" to PatternRecord("morning_home", "Launch Daily Briefing & Play Ambient Sounds", 0.88f),
        "headphones_connected" to PatternRecord("headphones_connected", "Continue Focus Deep Work Playlist", 0.82f),
        "low_battery" to PatternRecord("low_battery", "Engage Ultra Power Saving & Dim Display", 0.91f),
        "work_hours_desk" to PatternRecord("work_hours_desk", "Silence distracting notifications & open Linear", 0.79f)
    )

    fun recordAction(contextKey: String, action: String) {
        val existing = patterns[contextKey]
        if (existing != null) {
            val updatedProb = Math.min(0.99f, existing.probability + 0.03f)
            patterns[contextKey] = existing.copy(predictedAction = action, probability = updatedProb)
        } else {
            patterns[contextKey] = PatternRecord(contextKey, action, 0.76f)
        }
    }

    fun getPrediction(): String {
        val now = System.currentTimeMillis()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val contextKey = when {
            hour in 6..10 -> "morning_home"
            hour in 11..18 -> "work_hours_desk"
            else -> "headphones_connected"
        }

        val pattern = patterns[contextKey]
        if (pattern != null && pattern.probability >= 0.75f && now > pattern.suppressedUntilMs) {
            return JSONObject().apply {
                put("hasPrediction", true)
                put("patternId", pattern.contextKey)
                put("suggestedAction", pattern.predictedAction)
                put("confidence", pattern.probability)
                put("explanation", "Predicted with ${(pattern.probability * 100).toInt()}% confidence based on routine history.")
            }.toString()
        }

        return JSONObject().apply {
            put("hasPrediction", false)
        }.toString()
    }

    fun suppressPattern(patternId: String) {
        val p = patterns[patternId]
        if (p != null) {
            p.suppressedUntilMs = System.currentTimeMillis() + (24 * 60 * 60 * 1000L) // 24hr suppression
        }
    }
}
