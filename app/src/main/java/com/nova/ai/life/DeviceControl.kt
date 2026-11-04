package com.nova.ai.life

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings

object DeviceControl {

    private var isTorchOn = false

    fun torch(context: Context, on: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, on)
                isTorchOn = on
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun toggleTorch(context: Context): Boolean {
        return torch(context, !isTorchOn)
    }

    fun setVolume(context: Context, streamType: Int = AudioManager.STREAM_MUSIC, percent: Int): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val max = audioManager.getStreamMaxVolume(streamType)
            val target = ((percent / 100.0) * max).toInt().coerceIn(0, max)
            audioManager.setStreamVolume(streamType, target, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setBrightness(context: Context, percent: Int): Boolean {
        return try {
            val value = ((percent / 100.0) * 255).toInt().coerceIn(0, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setSilentMode(context: Context, silent: Boolean): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            audioManager.ringerMode = if (silent) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
            true
        } catch (e: Exception) {
            false
        }
    }

    fun lockScreen(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.lockNow()
            true
        } catch (e: Exception) {
            false
        }
    }
}
