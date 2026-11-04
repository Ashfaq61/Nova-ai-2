package com.nova.ai.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nova.ai.memory.NovaPrefs
import com.nova.ai.services.MeshNetworkService
import com.nova.ai.services.WakeWordService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device boot completed, initializing Nova AI background daemons")
            NovaPrefs.init(context)

            // Start Wake Word listener if enabled
            if (NovaPrefs.getBool("wake_word_enabled", true)) {
                val wakeIntent = Intent(context, WakeWordService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(wakeIntent)
                } else {
                    context.startService(wakeIntent)
                }
            }

            // Start Mesh Swarm node if enabled
            if (NovaPrefs.getBool("mesh_swarm_enabled", false)) {
                val meshIntent = Intent(context, MeshNetworkService::class.java)
                context.startService(meshIntent)
            }
        }
    }
}
