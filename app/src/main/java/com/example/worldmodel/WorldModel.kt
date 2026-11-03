package com.example.worldmodel

import org.json.JSONArray
import org.json.JSONObject

class WorldModel {

    fun simulatePreMortem(action: String, paramsJson: String): String {
        val sideEffects = JSONArray()
        var safetyScore = 0.95f
        var risk = "MINIMAL"

        val act = action.lowercase()
        when {
            act.contains("brightness") -> {
                sideEffects.put("Display brightness adjusted instantly")
                sideEffects.put("Slight alteration in battery discharge rate")
            }
            act.contains("tap") || act.contains("swipe") -> {
                sideEffects.put("UI focus will change in foreground app")
                sideEffects.put("Navigation stack may update")
                safetyScore = 0.90f
            }
            act.contains("torch") -> {
                sideEffects.put("Rear camera LED toggled")
                sideEffects.put("Minor battery draw increase")
            }
            else -> {
                sideEffects.put("Normal execution without external state side effects")
            }
        }

        return JSONObject().apply {
            put("action", action)
            put("safetyScore", safetyScore)
            put("riskLevel", risk)
            put("sideEffects", sideEffects)
            put("canProceed", safetyScore >= 0.80f)
        }.toString()
    }
}
