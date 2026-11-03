package com.nova.ai.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class NovaAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: NovaAccessibilityService? = null
            private set
        var lastPkg: String? = null
            private set
        
        fun global(action: Int): Boolean = instance?.performGlobalAction(action) ?: false
        
        fun tap(x: Float, y: Float): Boolean {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            return instance?.dispatchGesture(gesture, null, null) ?: false
        }

        fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
            return instance?.dispatchGesture(gesture, null, null) ?: false
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                lastPkg = it.packageName?.toString()
            }
        }
    }
    
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
