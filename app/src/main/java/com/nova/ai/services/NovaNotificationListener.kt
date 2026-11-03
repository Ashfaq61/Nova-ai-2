package com.nova.ai.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NovaNotificationListener : NotificationListenerService() {

    companion object {
        var instance: NovaNotificationListener? = null
            private set
        var lastNotificationText: String = ""
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i("NovaNotification", "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { notification ->
            val extras = notification.notification?.extras ?: return
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val pkg = notification.packageName ?: ""
            
            // Skip ongoing or empty notifications
            if (title.isNotEmpty() || text.isNotEmpty()) {
                lastNotificationText = "[$pkg] $title: $text"
                Log.d("NovaNotification", "Captured: $lastNotificationText")
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }
}
