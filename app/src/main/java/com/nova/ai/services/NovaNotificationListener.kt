package com.nova.ai.services

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.nova.ai.agent.AutomationEngine
import com.nova.ai.memory.NovaPrefs

class NovaNotificationListener : NotificationListenerService() {

    companion object {
        var instance: NovaNotificationListener? = null
            private set
        var lastNotificationText: String = ""
            private set
        var lastTriageScore: Int = 0
            private set

        fun replyToLast(replyText: String): Boolean {
            return instance?.replyToNotification(replyText) ?: false
        }
    }

    private var latestSbn: StatusBarNotification? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i("NovaNotification", "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { notification ->
            latestSbn = notification
            val extras = notification.notification?.extras ?: return
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val pkg = notification.packageName ?: ""

            if (title.isNotEmpty() || text.isNotEmpty()) {
                lastNotificationText = "[$pkg] $title: $text"
                lastTriageScore = computeTriageScore(pkg, title, text)
                Log.d("NovaNotification", "Captured: $lastNotificationText (Triage: $lastTriageScore)")

                // Trigger automation engine
                AutomationEngine.onNotificationReceived(applicationContext, pkg, text)

                // Read aloud if setting enabled and triage score is urgent (>60)
                if (NovaPrefs.getBool("notif_read_aloud", false) && lastTriageScore >= 60) {
                    // Queue speech via TTS
                }
            }
        }
    }

    fun computeTriageScore(pkg: String, title: String, text: String): Int {
        var score = 30 // Baseline score
        val combined = "$title $text".lowercase()

        // Package urgency
        if (pkg.contains("whatsapp") || pkg.contains("messaging") || pkg.contains("telegram") || pkg.contains("dialer")) {
            score += 25
        }

        // Keywords
        if (combined.contains("urgent") || combined.contains("emergency") || combined.contains("asap") || combined.contains("call me")) {
            score += 35
        }
        if (combined.contains("otp") || combined.contains("code") || combined.contains("verification")) {
            score += 30
        }
        if (combined.contains("sale") || combined.contains("discount") || combined.contains("promo") || combined.contains("newsletter")) {
            score -= 20
        }

        return score.coerceIn(0, 100)
    }

    fun replyToNotification(replyText: String): Boolean {
        val sbn = latestSbn ?: return false
        val actions = sbn.notification?.actions ?: return false

        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (input in remoteInputs) {
                val intent = Intent()
                val bundle = Bundle().apply {
                    putCharSequence(input.resultKey, replyText)
                }
                RemoteInput.addResultsToIntent(arrayOf(input), intent, bundle)
                return try {
                    action.actionIntent.send(this, 0, intent)
                    Log.i("NovaNotification", "Auto-replied via RemoteInput: $replyText")
                    true
                } catch (e: Exception) {
                    Log.e("NovaNotification", "RemoteInput reply failed", e)
                    false
                }
            }
        }
        return false
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }
}
