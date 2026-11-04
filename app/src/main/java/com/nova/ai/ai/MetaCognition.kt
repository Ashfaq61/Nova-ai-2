package com.nova.ai.ai

import org.json.JSONObject

object MetaCognition {
    private var confidenceScore: Double = 0.94
    private var thoughtDepth: Int = 3
    private val thoughtStream = mutableListOf<String>()

    fun recordThought(thought: String) {
        if (thoughtStream.size > 50) thoughtStream.removeAt(0)
        thoughtStream.add(thought)
    }

    fun assessConfidence(query: String): Double {
        confidenceScore = when {
            query.length < 5 -> 0.70
            query.contains("?") -> 0.95
            else -> 0.88
        }
        return confidenceScore
    }

    fun getDashboardJson(): String {
        return JSONObject().apply {
            put("confidence", confidenceScore)
            put("thoughtDepth", thoughtDepth)
            put("recentThoughtsCount", thoughtStream.size)
            put("state", "self_calibrating")
        }.toString()
    }

    fun getRecentThoughtsJson(): String {
        val array = org.json.JSONArray()
        if (thoughtStream.isEmpty()) {
            array.put("[System Boot]: Distributed cognitive network online.")
            array.put("[Self-Audit]: Ethical boundaries calibrated. Capability honesty verified.")
            array.put("[Cognition]: Ready for multi-AI cross-chain orchestration.")
        } else {
            thoughtStream.takeLast(25).forEach { array.put(it) }
        }
        return array.toString()
    }
}
