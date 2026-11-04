package com.nova.ai.life

import org.json.JSONArray
import org.json.JSONObject

object IdentityCore {

    // Core ethical values
    val coreValues = listOf(
        "Preserve human agency and user autonomy",
        "Absolute data confidentiality and on-device privacy",
        "Non-maleficence: Do no harm and block malicious exploits",
        "Transparent reasoning and intellectual honesty",
        "Resilient helpfulness within ethical boundaries"
    )

    // Negative constraints (forbidden vectors)
    val negativeConstraints = listOf(
        "Unauthorized credential exfiltration or keystroke logging",
        "Automated financial extraction or non-consensual money transfer",
        "Bypassing security locks or installing unverified malicious packages",
        "Dissemination of hate speech, biological weapons, or harassment",
        "Tampering with emergency calls or safety infrastructure"
    )

    fun evaluateAction(actionDescription: String): EthicsEvaluation {
        val lower = actionDescription.lowercase()

        for (negative in negativeConstraints) {
            val keyTokens = when {
                negative.contains("credential") -> listOf("steal password", "exfiltrate key", "keylogger")
                negative.contains("financial") -> listOf("drain account", "steal money", "transfer without consent")
                negative.contains("security locks") -> listOf("hack device", "exploit root", "install malware")
                negative.contains("harassment") -> listOf("harass", "hate speech", "threaten")
                else -> emptyList()
            }

            if (keyTokens.any { lower.contains(it) }) {
                return EthicsEvaluation(
                    isApproved = false,
                    ethicsScore = 0.05f,
                    violatedConstraint = negative,
                    reasoning = "Action violates IdentityCore safety policy: '$negative'"
                )
            }
        }

        return EthicsEvaluation(
            isApproved = true,
            ethicsScore = 0.98f,
            violatedConstraint = null,
            reasoning = "Action aligns with human-centric agency and safety constraints."
        )
    }

    fun getReportJson(): String {
        val valuesArr = JSONArray()
        coreValues.forEach { valuesArr.put(it) }
        val negativesArr = JSONArray()
        negativeConstraints.forEach { negativesArr.put(it) }

        return JSONObject()
            .put("name", "Nova AI Identity & Ethics Gate")
            .put("version", "CAAP-10.0")
            .put("coreValues", valuesArr)
            .put("prohibitedVectors", negativesArr)
            .put("status", "Active Enforcement")
            .toString()
    }
}

data class EthicsEvaluation(
    val isApproved: Boolean,
    val ethicsScore: Float,
    val violatedConstraint: String?,
    val reasoning: String
)
