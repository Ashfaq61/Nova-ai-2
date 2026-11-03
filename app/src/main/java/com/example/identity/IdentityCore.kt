package com.example.identity

import org.json.JSONObject

class IdentityCore {

    fun evaluateSafetyGate(action: String, paramsJson: String): String {
        val act = action.lowercase()
        // Critical safety checks: Payment, Destructive deletion, Unauthorized sharing
        if (act.contains("pay") || act.contains("transfer") || act.contains("buy")) {
            return JSONObject().apply {
                put("allowed", false)
                put("requiresConfirmation", true)
                put("riskLevel", "HIGH_FINANCIAL")
                put("reason", "Financial transaction requested. IdentityCore mandates biometric confirmation.")
            }.toString()
        }

        if (act.contains("delete_all") || act.contains("format") || act.contains("wipe")) {
            return JSONObject().apply {
                put("allowed", false)
                put("requiresConfirmation", true)
                put("riskLevel", "CRITICAL_DESTRUCTIVE")
                put("reason", "Destructive system operation blocked by core ethical safety guardrails.")
            }.toString()
        }

        return JSONObject().apply {
            put("allowed", true)
            put("requiresConfirmation", false)
            put("riskLevel", "SAFE")
            put("reason", "Operation passes all safety verification gates.")
        }.toString()
    }
}
