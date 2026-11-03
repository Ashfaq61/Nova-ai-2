package com.nova.ai.utils

object PrivacyScrubber {
    private val tokenMap = mutableMapOf<String, String>()
    
    fun scrub(text: String): String {
        var s = text
        tokenMap.clear()
        // Simple PII patterns (phone and email)
        listOf(
            Regex("\\b\\d{10}\\b") to "PHONE",
            Regex("[\\w.+-]+@[\\w-]+\\.\\w+") to "EMAIL"
        ).forEachIndexed { idx, (pattern, tag) ->
            pattern.findAll(s).forEach { match ->
                val token = "<$tag$idx>"
                tokenMap[token] = match.value
                s = s.replace(match.value, token)
            }
        }
        return s
    }
    
    fun restore(text: String): String {
        var s = text
        tokenMap.forEach { (token, original) -> 
            s = s.replace(token, original) 
        }
        return s
    }
}
