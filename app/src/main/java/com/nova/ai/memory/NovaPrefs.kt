package com.nova.ai.memory

import android.content.Context
import android.content.SharedPreferences

object NovaPrefs {
    private const val PREFS_NAME = "nova_ai_prefs"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun setKey(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    fun getKey(key: String, default: String = ""): String {
        return prefs?.getString(key, default) ?: default
    }

    fun setBool(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    fun getBool(key: String, default: Boolean = false): Boolean {
        return prefs?.getBoolean(key, default) ?: default
    }

    fun setPersonality(personality: String) {
        setKey("nova_personality", personality)
    }

    fun getPersonality(): String {
        return getKey("nova_personality", "Professional")
    }
}
