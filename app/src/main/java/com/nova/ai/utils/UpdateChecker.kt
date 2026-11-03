package com.nova.ai.utils

import org.json.JSONObject

object UpdateChecker {
    const val CURRENT_VERSION_CODE = 10
    const val CURRENT_VERSION_NAME = "1.0.0-phase10"

    fun checkForUpdates(): String {
        return JSONObject().apply {
            put("currentVersion", CURRENT_VERSION_NAME)
            put("hasUpdate", false)
            put("status", "Up to date")
        }.toString()
    }
}
