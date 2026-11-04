package com.nova.ai.sensing

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PrivacyScrubber {

    private val tokenVault = ConcurrentHashMap<String, String>()
    private val auditLogs = mutableListOf<String>()

    private val EMAIL_REGEX = Regex("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+")
    private val PHONE_REGEX = Regex("(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}")
    private val CREDIT_CARD_REGEX = Regex("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})\\b")
    private val IP_REGEX = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")

    fun scrub(input: String): String {
        var scrubbed = input
        var matchesCount = 0

        fun replacePattern(regex: Regex, prefix: String) {
            scrubbed = regex.replace(scrubbed) { matchResult ->
                val realValue = matchResult.value
                val token = "<$prefix:${UUID.randomUUID().toString().take(8)}>"
                tokenVault[token] = realValue
                matchesCount++
                token
            }
        }

        replacePattern(EMAIL_REGEX, "REDACTED_EMAIL")
        replacePattern(PHONE_REGEX, "REDACTED_PHONE")
        replacePattern(CREDIT_CARD_REGEX, "REDACTED_CARD")
        replacePattern(IP_REGEX, "REDACTED_IP")

        if (matchesCount > 0) {
            val logEntry = "[AUDIT] Scrubbed $matchesCount PII item(s) from input string"
            auditLogs.add(logEntry)
        }

        return scrubbed
    }

    fun restore(scrubbedText: String): String {
        var restored = scrubbedText
        tokenVault.forEach { (token, realValue) ->
            if (restored.contains(token)) {
                restored = restored.replace(token, realValue)
            }
        }
        return restored
    }

    fun getAuditLogJson(): String {
        val arr = JSONArray()
        auditLogs.takeLast(20).forEach { arr.put(it) }
        return JSONObject()
            .put("auditEntries", arr)
            .put("totalVaultTokens", tokenVault.size)
            .toString()
    }
}
