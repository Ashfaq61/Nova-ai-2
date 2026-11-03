package com.example.biometrics

import org.json.JSONObject
import kotlin.random.Random

class BioFusion {
    private var isRegistered = false

    fun registerBioSensors(): Boolean {
        isRegistered = true
        return true
    }

    fun getHeartRate(): Int {
        // Simulated PPG green channel peak detection (68 - 84 BPM baseline)
        return Random.nextInt(68, 82)
    }

    fun getStressIndex(): Float {
        // HRV RR-interval standard deviation stress score (0.0 to 1.0)
        return (Random.nextInt(18, 38) / 100f)
    }

    fun getMoodVector(): String {
        val hr = getHeartRate()
        val stress = getStressIndex()
        val mood = when {
            stress > 0.65f -> "Stressed"
            stress < 0.25f && hr < 75 -> "Calm & Focused"
            hr > 85 -> "Excited"
            else -> "Attentive & Balanced"
        }

        return JSONObject().apply {
            put("heartRateBpm", hr)
            put("stressIndex", stress)
            put("mood", mood)
            put("microExpressionSmile", 0.72)
            put("tremorDelta", 0.04)
            put("activePrivacyGate", true)
        }.toString()
    }
}
