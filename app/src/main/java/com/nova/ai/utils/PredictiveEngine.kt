package com.nova.ai.utils

import org.json.JSONArray
import org.json.JSONObject

object PredictiveEngine {
    private val actionHistory = mutableListOf<String>()

    fun recordAction(action: String) {
        if (actionHistory.size > 100) actionHistory.removeAt(0)
        actionHistory.add(action)
    }

    fun predictNextAction(): String {
        if (actionHistory.isEmpty()) return "ask_greeting"
        val last = actionHistory.last()
        return when (last) {
            "torch_on" -> "torch_off"
            "music_play" -> "volume_control"
            else -> "show_suggestions"
        }
    }

    fun getSuggestions(): String {
        return JSONObject().apply {
            put("predictions", JSONArray().put("Torch").put("Home").put("Apps").put("Play music"))
        }.toString()
    }
}
