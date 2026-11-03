package com.nova.ai.ai

object UniversalTranslator {
    private val quickDictionary = mapOf(
        "hello" to mapOf("es" to "hola", "hi" to "नमस्ते", "fr" to "bonjour", "ja" to "こんにちは"),
        "thank you" to mapOf("es" to "gracias", "hi" to "धन्यवाद", "fr" to "merci", "ja" to "ありがとう"),
        "how are you" to mapOf("es" to "¿cómo estás?", "hi" to "आप कैसे हैं?", "fr" to "comment ça va?", "ja" to "お元気ですか")
    )

    fun translate(text: String, sourceLang: String = "auto", targetLang: String = "es"): String {
        val lower = text.trim().lowercase()
        val mapped = quickDictionary[lower]?.get(targetLang)
        return mapped ?: "[$targetLang translation]: $text"
    }
}
