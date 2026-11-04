package com.nova.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nova.ai.agent.AutomationEngine
import com.nova.ai.life.DreamWorker
import com.nova.ai.memory.KnowledgeGraph
import com.nova.ai.memory.MemoryDb
import com.nova.ai.memory.NovaPrefs

class NovaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // Initialize core cognitive subsystems
        NovaPrefs.init(this)
        MemoryDb.getInstance(this)
        KnowledgeGraph.getInstance(this)
        AutomationEngine.init(this)

        // Schedule nightly cognitive dream consolidation
        try {
            DreamWorker.schedule(this)
        } catch (e: Exception) {
            // Ignored in test JVM
        }
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
