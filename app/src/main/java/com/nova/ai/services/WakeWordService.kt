package com.nova.ai.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nova.ai.MainActivity
import java.util.Locale

class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        var isListeningForWakeWord: Boolean = false
            private set
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        initSpeechRecognizer()
    }

    private fun startInForeground() {
        val channelId = "nova_wake_word_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nova Wake Word Listener",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Nova AI Continuous Listener")
            .setContentText("Say 'Hey Nova' to activate")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            restartListening(100)
                        }

                        override fun onError(error: Int) {
                            // Recover from audio/timeout errors and restart
                            restartListening(600)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            matches?.forEach { match ->
                                val lower = match.lowercase().trim()
                                Log.d(TAG, "Recognized audio: $lower")
                                if (lower.contains("hey nova") || lower.contains("hay nova")) {
                                    triggerWakeAction(active = true)
                                } else if (lower.contains("bye nova") || lower.contains("goodbye nova")) {
                                    triggerWakeAction(active = false)
                                }
                            }
                            restartListening(100)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })

                    startListeningLoop()
                }
            } catch (e: Exception) {
                Log.e(TAG, "SpeechRecognizer initialization error", e)
            }
        }
    }

    private fun startListeningLoop() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechRecognizer?.startListening(intent)
            isListeningForWakeWord = true
        } catch (e: Exception) {
            restartListening(1000)
        }
    }

    private fun restartListening(delayMs: Long) {
        mainHandler.postDelayed({
            if (isListeningForWakeWord) {
                startListeningLoop()
            }
        }, delayMs)
    }

    private fun triggerWakeAction(active: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("WAKE_TRIGGER", if (active) "WAKE_UP" else "SLEEP")
        }
        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isListeningForWakeWord = false
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
