package com.example.emotion

import org.json.JSONObject

class EmotionEngine {
    private var voiceMode: String = "Nova Default"
    private var isEnrolled: Boolean = false

    fun enrollVoice(sampleMeta: String): Boolean {
        isEnrolled = true
        voiceMode = "Your Clone"
        return true
    }

    fun setVoiceMode(mode: String) {
        voiceMode = mode
    }

    fun getVoiceMode(): String = voiceMode

    fun getEmotionTTSParams(detectedMood: String): String {
        val pitch: Float
        val rate: Float
        val tone: String

        when (detectedMood.lowercase()) {
            "happy" -> {
                pitch = 1.15f
                rate = 1.10f
                tone = "Bright, energetic, warm"
            }
            "sad" -> {
                pitch = 0.80f
                rate = 0.85f
                tone = "Gentle, supportive, empathetic"
            }
            "angry" -> {
                pitch = 0.90f
                rate = 1.15f
                tone = "Crisp, concise, reassuring"
            }
            "stressed" -> {
                pitch = 0.95f
                rate = 0.90f
                tone = "Calming, composed, deliberate"
            }
            else -> {
                pitch = 1.0f
                rate = 1.0f
                tone = "Poised, balanced, articulate"
            }
        }

        return JSONObject().apply {
            put("mode", voiceMode)
            put("pitch", pitch)
            put("rate", rate)
            put("tone", tone)
            put("isCloneActive", isEnrolled && voiceMode == "Your Clone")
        }.toString()
    }
}
