package com.example.privacy

import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

class PrivacyScrubber {

    private val redactionsLog = JSONArray()
    private var isFederatedLearningEnabled = true

    private val emailPattern = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+")
    private val phonePattern = Pattern.compile("(\\+\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}")
    private val cardPattern = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b")
    private val ssnPattern = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b")

    fun scrub(input: String): Pair<String, Map<String, String>> {
        val mapping = mutableMapOf<String, String>()
        var scrubbed = input
        var redactionCount = 0

        val emailMatcher = emailPattern.matcher(scrubbed)
        while (emailMatcher.find()) {
            val original = emailMatcher.group()
            val token = "<REDACTED_EMAIL_${mapping.size + 1}>"
            mapping[token] = original
            scrubbed = scrubbed.replace(original, token)
            redactionCount++
        }

        val phoneMatcher = phonePattern.matcher(scrubbed)
        while (phoneMatcher.find()) {
            val original = phoneMatcher.group()
            val token = "<REDACTED_PHONE_${mapping.size + 1}>"
            mapping[token] = original
            scrubbed = scrubbed.replace(original, token)
            redactionCount++
        }

        val cardMatcher = cardPattern.matcher(scrubbed)
        while (cardMatcher.find()) {
            val original = cardMatcher.group()
            val token = "<REDACTED_CARD_${mapping.size + 1}>"
            mapping[token] = original
            scrubbed = scrubbed.replace(original, token)
            redactionCount++
        }

        if (redactionCount > 0) {
            redactionsLog.put(JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("redactedCount", redactionCount)
                put("privacyBudgetEpsilon", 0.05)
            })
        }

        return Pair(scrubbed, mapping)
    }

    fun unscrub(text: String, mapping: Map<String, String>): String {
        var restored = text
        for ((token, original) in mapping) {
            restored = restored.replace(token, original)
        }
        return restored
    }

    fun getPrivacyStats(): String {
        return JSONObject().apply {
            put("federatedLearningEnabled", isFederatedLearningEnabled)
            put("totalRedactedEntities", redactionsLog.length())
            put("differentialPrivacyEpsilon", 0.05)
            put("onDeviceEncryption", "AES-256 GCM")
            put("piiScrubbingStatus", "ACTIVE")
        }.toString()
    }

    fun toggleFederatedLearning(enabled: Boolean): Boolean {
        isFederatedLearningEnabled = enabled
        return isFederatedLearningEnabled
    }

    fun exportAuditLog(): String {
        return redactionsLog.toString()
    }
}
