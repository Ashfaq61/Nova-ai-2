package com.nova.ai.sensing

import org.json.JSONObject

object EmotionEngine {
    // Valence (-1.0 to +1.0) and Arousal (0.0 to 1.0)
    private var currentValence: Float = 0.2f
    private var currentArousal: Float = 0.3f
    private const val ALPHA = 0.25f // Smoothing factor for Exponential Moving Average (EMA)

    fun updateObservation(observedValence: Float, observedArousal: Float): EmotionState {
        // Apply EMA filter: S_t = alpha * Y_t + (1 - alpha) * S_{t-1}
        currentValence = (ALPHA * observedValence + (1f - ALPHA) * currentValence).coerceIn(-1.0f, 1.0f)
        currentArousal = (ALPHA * observedArousal + (1f - ALPHA) * currentArousal).coerceIn(0.0f, 1.0f)
        return getMoodState()
    }

    fun analyzeSentimentText(text: String): EmotionState {
        val lower = text.lowercase()
        var valence = 0.0f
        var arousal = 0.2f

        when {
            lower.contains("happy") || lower.contains("love") || lower.contains("great") || lower.contains("amazing") -> {
                valence = 0.8f
                arousal = 0.7f
            }
            lower.contains("angry") || lower.contains("hate") || lower.contains("terrible") || lower.contains("urgent") -> {
                valence = -0.8f
                arousal = 0.85f
            }
            lower.contains("sad") || lower.contains("tired") || lower.contains("exhausted") || lower.contains("depressed") -> {
                valence = -0.6f
                arousal = 0.15f
            }
            lower.contains("relax") || lower.contains("calm") || lower.contains("peace") -> {
                valence = 0.5f
                arousal = 0.1f
            }
        }
        return updateObservation(valence, arousal)
    }

    fun getMoodState(): EmotionState {
        val label = when {
            currentValence > 0.3f && currentArousal > 0.5f -> "Euphoric / Energized"
            currentValence > 0.2f && currentArousal <= 0.5f -> "Calm / Content"
            currentValence < -0.3f && currentArousal > 0.5f -> "Frustrated / Agitated"
            currentValence < -0.2f && currentArousal <= 0.5f -> "Drained / Melancholic"
            else -> "Focused / Neutral Equilibrium"
        }

        return EmotionState(
            valence = currentValence,
            arousal = currentArousal,
            label = label
        )
    }

    fun toJson(): String {
        val state = getMoodState()
        return JSONObject()
            .put("valence", (state.valence * 100).toInt() / 100.0)
            .put("arousal", (state.arousal * 100).toInt() / 100.0)
            .put("label", state.label)
            .toString()
    }
}

data class EmotionState(
    val valence: Float,
    val arousal: Float,
    val label: String
)
