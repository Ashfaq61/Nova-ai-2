package com.nova.ai.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NovaAccessibilityService : AccessibilityService() {

    companion object {
        var instance: NovaAccessibilityService? = null
            private set
        var lastPkg: String? = null
            private set
        var lastScreenText: String = ""
            private set

        fun global(action: Int): Boolean = instance?.performGlobalAction(action) ?: false

        fun globalActions(actionName: String): Boolean {
            val inst = instance ?: return false
            return when (actionName.lowercase().trim()) {
                "back" -> inst.performGlobalAction(GLOBAL_ACTION_BACK)
                "home" -> inst.performGlobalAction(GLOBAL_ACTION_HOME)
                "recents" -> inst.performGlobalAction(GLOBAL_ACTION_RECENTS)
                "notifications" -> inst.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                "quick_settings" -> inst.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                "lock" -> inst.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                else -> false
            }
        }

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

        fun typeText(text: String): Boolean {
            val inst = instance ?: return false
            val root = inst.rootInActiveWindow ?: return false
            val focusNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: root
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focusNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        fun readScreen(): String {
            val inst = instance ?: return ""
            val root = inst.rootInActiveWindow ?: return ""
            val sb = StringBuilder()

            fun traverse(node: AccessibilityNodeInfo?) {
                if (node == null) return
                val text = node.text ?: node.contentDescription
                if (!text.isNullOrBlank()) {
                    sb.append(text).append("\n")
                }
                for (i in 0 until node.childCount) {
                    traverse(node.getChild(i))
                }
            }

            traverse(root)
            val fullText = sb.toString().trim()
            lastScreenText = fullText
            return fullText
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
