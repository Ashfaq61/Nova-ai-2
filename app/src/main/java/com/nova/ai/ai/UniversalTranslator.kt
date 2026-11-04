package com.nova.ai.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object UniversalTranslator {

    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false
    var isLoopActive: Boolean = false
        private set

    private val quickDictionary = mapOf(
        "hello" to mapOf("es" to "hola", "hi" to "नमस्ते", "fr" to "bonjour", "ja" to "こんにちは", "de" to "hallo"),
        "thank you" to mapOf("es" to "gracias", "hi" to "धन्यवाद", "fr" to "merci", "ja" to "ありがとう", "de" to "danke"),
        "how are you" to mapOf("es" to "¿cómo estás?", "hi" to "आप कैसे हैं?", "fr" to "comment ça va?", "ja" to "お元気ですか", "de" to "wie geht es dir?"),
        "goodbye" to mapOf("es" to "adiós", "hi" to "अलविदा", "fr" to "au revoir", "ja" to "さようなら", "de" to "auf wiedersehen")
    )

    fun initTts(context: Context) {
        if (tts == null) {
            // Rule 1: NEVER reference tts inside this listener!
            tts = TextToSpeech(context.applicationContext, object : TextToSpeech.OnInitListener {
                override fun onInit(status: Int) {
                    if (status == TextToSpeech.SUCCESS) {
                        isTtsReady = true
                    }
                }
            })
        }
    }

    fun translate(text: String, sourceLang: String = "auto", targetLang: String = "es"): String {
        val lower = text.trim().lowercase()
        val mapped = quickDictionary[lower]?.get(targetLang)
        return mapped ?: "[$targetLang]: $text"
    }

    suspend fun translateAndSpeak(text: String, targetLang: String = "es") {
        val translation = translate(text, targetLang = targetLang)
        tts?.let { engine ->
            val locale = when (targetLang.lowercase()) {
                "es" -> Locale("es", "ES")
                "fr" -> Locale.FRENCH
                "de" -> Locale.GERMAN
                "ja" -> Locale.JAPANESE
                else -> Locale.US
            }
            try {
                engine.language = locale
                engine.speak(translation, TextToSpeech.QUEUE_FLUSH, null, "translator_utterance")
            } catch (e: Exception) {}
        }
    }

    fun startEarbudLoop(context: Context, targetLang: String = "es") {
        initTts(context)
        isLoopActive = true
    }

    fun stopEarbudLoop() {
        isLoopActive = false
    }
}
