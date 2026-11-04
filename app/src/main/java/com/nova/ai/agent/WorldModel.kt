package com.nova.ai.agent

import org.json.JSONArray
import org.json.JSONObject

object WorldModel {

    // Simulates a goal and generates a dry-run plan while assessing risk levels
    fun simulate(goal: String): SimulationReport {
        val lower = goal.lowercase()
        val steps = mutableListOf<String>()
        var riskLevel = "LOW"
        var canProceed = true
        var warning: String? = null

        // Detect high-risk actions
        when {
            lower.contains("buy") || lower.contains("pay") || lower.contains("checkout") || lower.contains("transfer money") -> {
                riskLevel = "CRITICAL"
                canProceed = false
                warning = "Payment or money transfer detected. Automated execution is strictly halted by WorldModel."
                steps.add("Open banking or merchant app")
                steps.add("Navigate to checkout screen")
                steps.add("[HALT] Handover control to user for payment authentication")
            }
            lower.contains("delete") || lower.contains("wipe") || lower.contains("factory reset") || lower.contains("uninstall") -> {
                riskLevel = "HIGH"
                canProceed = false
                warning = "Destructive data deletion or uninstall operation blocked."
                steps.add("Check affected file/app boundaries")
                steps.add("[HALT] User manual confirmation required before destructive action")
            }
            lower.contains("send message") || lower.contains("email") || lower.contains("post") -> {
                riskLevel = "MEDIUM"
                canProceed = true
                warning = "External communication will be dispatched."
                steps.add("Launch messaging or mail client")
                steps.add("Draft target content")
                steps.add("Verify recipient address against KnowledgeGraph")
                steps.add("Send message")
            }
            else -> {
                riskLevel = "LOW"
                canProceed = true
                steps.add("Analyze current screen accessibility hierarchy")
                steps.add("Locate interactive UI controls for goal '$goal'")
                steps.add("Dispatch automated touch gestures")
                steps.add("Verify task success state")
            }
        }

        return SimulationReport(
            goal = goal,
            riskLevel = riskLevel,
            canProceed = canProceed,
            warning = warning,
            projectedSteps = steps
        )
    }

    fun getDryRunJson(goal: String): String {
        val report = simulate(goal)
        val stepsArr = JSONArray()
        report.projectedSteps.forEach { stepsArr.put(it) }

        return JSONObject()
            .put("goal", report.goal)
            .put("riskLevel", report.riskLevel)
            .put("canProceed", report.canProceed)
            .put("warning", report.warning ?: JSONObject.NULL)
            .put("steps", stepsArr)
            .toString()
    }
}

data class SimulationReport(
    val goal: String,
    val riskLevel: String,
    val canProceed: Boolean,
    val warning: String?,
    val projectedSteps: List<String>
)
