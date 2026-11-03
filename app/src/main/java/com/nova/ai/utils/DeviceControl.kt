package com.nova.ai.utils

import android.content.Context
import android.hardware.camera2.CameraManager

object DeviceControl {
    fun torch(ctx: Context, on: Boolean): Boolean = try {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull() ?: "0"
        cm.setTorchMode(id, on)
        true
    } catch (e: Exception) {
        false
    }
}
