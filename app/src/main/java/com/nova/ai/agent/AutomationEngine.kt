package com.nova.ai.agent

import android.content.Context
import com.nova.ai.life.DeviceControl
import com.nova.ai.life.RoutineEngine
import org.json.JSONArray
import org.json.JSONObject

object AutomationEngine {

    private val rules = mutableListOf<AutomationRule>()

    fun init(context: Context) {
        if (rules.isEmpty()) {
            // Seed sample intelligent automation rules
            rules.add(
                AutomationRule(
                    id = "rule_battery_saver",
                    triggerType = "battery",
                    triggerValue = "<20",
                    action = "routine:battery_saver"
                )
            )
            rules.add(
                AutomationRule(
                    id = "rule_night_silence",
                    triggerType = "time",
                    triggerValue = "22:30",
                    action = "device:silent"
                )
            )
            rules.add(
                AutomationRule(
                    id = "rule_whatsapp_auto",
                    triggerType = "notif",
                    triggerValue = "com.whatsapp",
                    action = "routine:read_notif"
                )
            )
        }
    }

    fun addRule(triggerType: String, triggerValue: String, action: String): String {
        val id = "rule_${System.currentTimeMillis()}"
        rules.add(AutomationRule(id, triggerType, triggerValue, action))
        return id
    }

    fun onBatteryChanged(context: Context, level: Int) {
        rules.filter { it.triggerType == "battery" && it.enabled }.forEach { rule ->
            val threshold = rule.triggerValue.removePrefix("<").removePrefix(">").toIntOrNull() ?: 20
            if (rule.triggerValue.startsWith("<") && level <= threshold) {
                executeAction(context, rule.action)
            } else if (rule.triggerValue.startsWith(">") && level >= threshold) {
                executeAction(context, rule.action)
            }
        }
    }

    fun onAppOpened(context: Context, packageName: String) {
        rules.filter { it.triggerType == "app_open" && it.enabled }.forEach { rule ->
            if (rule.triggerValue.equals(packageName, ignoreCase = true)) {
                executeAction(context, rule.action)
            }
        }
    }

    fun onNotificationReceived(context: Context, pkg: String, text: String) {
        rules.filter { it.triggerType == "notif" && it.enabled }.forEach { rule ->
            if (pkg.contains(rule.triggerValue) || text.contains(rule.triggerValue, ignoreCase = true)) {
                executeAction(context, rule.action)
            }
        }
    }

    fun checkTimeTriggers(context: Context, currentHour: Int, currentMinute: Int) {
        val timeString = String.format("%02d:%02d", currentHour, currentMinute)
        rules.filter { it.triggerType == "time" && it.enabled }.forEach { rule ->
            if (rule.triggerValue == timeString) {
                executeAction(context, rule.action)
            }
        }
    }

    private fun executeAction(context: Context, action: String) {
        when {
            action.startsWith("routine:") -> {
                val routineName = action.removePrefix("routine:")
                RoutineEngine.runRoutine(context, routineName)
            }
            action == "device:silent" -> {
                DeviceControl.setSilentMode(context, true)
            }
            action == "device:torch" -> {
                DeviceControl.toggleTorch(context)
            }
        }
    }

    fun getRulesJson(): String {
        val arr = JSONArray()
        rules.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("triggerType", r.triggerType)
                    .put("triggerValue", r.triggerValue)
                    .put("action", r.action)
                    .put("enabled", r.enabled)
            )
        }
        return arr.toString()
    }
}

data class AutomationRule(
    val id: String,
    val triggerType: String,
    val triggerValue: String,
    val action: String,
    var enabled: Boolean = true
)
