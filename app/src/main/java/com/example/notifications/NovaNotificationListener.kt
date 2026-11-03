package com.example.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NovaNotificationListener : NotificationListenerService() {

    companion object {
        var instance: NovaNotificationListener? = null
            private set

        var onNotificationCallback: ((title: String, text: String, pkg: String) -> Unit)? = null

        fun isConnected(): Boolean = instance != null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val n = sbn?.notification ?: return
        val extras = n.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val pkg = sbn.packageName ?: ""

        if (title.isNotEmpty() || text.isNotEmpty()) {
            onNotificationCallback?.invoke(title, text, pkg)
        }
    }

    fun quickReply(sbnKey: String, replyText: String): Boolean {
        val sbn = activeNotifications?.firstOrNull { it.key == sbnKey } ?: return false
        val actions = sbn.notification?.actions ?: return false

        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                val intent = Intent()
                val bundle = Bundle()
                bundle.putCharSequence(remoteInput.resultKey, replyText)
                RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                try {
                    action.actionIntent.send(this, 0, intent)
                    return true
                } catch (_: PendingIntent.CanceledException) {
                    return false
                }
            }
        }
        return false
    }
}
