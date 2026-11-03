package com.nova.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class NovaApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val channel = NotificationChannel(
                "nova_general",
                "Nova AI General",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General notifications from Nova AI"
            }
            
            manager?.createNotificationChannel(channel)
        }
    }
}
