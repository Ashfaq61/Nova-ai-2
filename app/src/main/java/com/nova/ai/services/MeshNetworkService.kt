package com.nova.ai.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class MeshNetworkService : Service() {

    companion object {
        private const val TAG = "NovaMeshService"
        var isMeshActive: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isMeshActive = true
        Log.i(TAG, "Nova Mesh P2P service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isMeshActive = false
        Log.i(TAG, "Nova Mesh P2P service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
