package com.example.services

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class WakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    companion object {
        var onWakeWordDetected: ((String) -> Unit)? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startContinuousRecognizer()
        return START_STICKY
    }

    private fun startContinuousRecognizer() {
        if (isListening) return
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            }
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }

                override fun onError(error: Int) {
                    isListening = false
                    restartListeningWithDelay()
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
                    if (spoken.contains("hey nova") || spoken.contains("ok nova")) {
                        onWakeWordDetected?.invoke("wake")
                    } else if (spoken.contains("bye nova") || spoken.contains("goodbye nova")) {
                        onWakeWordDetected?.invoke("sleep")
                    }
                    restartListeningWithDelay()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            restartListeningWithDelay()
        }
    }

    private fun restartListeningWithDelay() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startContinuousRecognizer()
        }, 1200)
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        super.onDestroy()
    }
}
