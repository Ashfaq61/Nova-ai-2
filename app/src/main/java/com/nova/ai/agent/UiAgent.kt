package com.nova.ai.agent

import com.nova.ai.ai.LlmManager
import com.nova.ai.services.NovaAccessibilityService
import com.nova.ai.services.ScreenCaptureService
import kotlinx.coroutines.delay
import org.json.JSONObject

object UiAgent {

    private const val MAX_STEPS = 12
    private var isRunning = false

    suspend fun executeGoal(goal: String, onProgress: (String) -> Unit = {}): AgentResult {
        if (isRunning) {
            return AgentResult(false, "Another UI Agent execution is currently active.")
        }
        isRunning = true

        try {
            // First run pre-mortem simulation via WorldModel
            val sim = WorldModel.simulate(goal)
            if (!sim.canProceed) {
                return AgentResult(false, "WorldModel Safety Gate Blocked: ${sim.warning}")
            }

            onProgress("Starting UI execution for: $goal")

            for (step in 1..MAX_STEPS) {
                onProgress("Step $step/$MAX_STEPS: Analyzing screen state...")

                // Check active screen text for payment or sensitive banking markers
                val screenText = NovaAccessibilityService.readScreen()
                if (isPaymentScreen(screenText)) {
                    return AgentResult(
                        success = false,
                        message = "[SAFETY HALT] Payment/Checkout screen detected at step $step. Handing control to user."
                    )
                }

                // Check if screen capture base64 is available
                val base64Img = ScreenCaptureService.capture()

                // Synthesize prompt for action determination
                val sysPrompt = """
                    You are Nova Android UI Agent. Return ONLY a single JSON object specifying the next action:
                    {"action": "tap", "x": 500, "y": 800} or
                    {"action": "type", "text": "Hello"} or
                    {"action": "swipe", "startX": 500, "startY": 1200, "endX": 500, "endY": 400} or
                    {"action": "done", "summary": "Goal finished"}
                """.trimIndent()

                val userPrompt = "Goal: $goal\nStep: $step\nActive Package: ${NovaAccessibilityService.lastPkg}\nScreen Content:\n${screenText.take(600)}"
                val actionJsonStr = LlmManager.ask(userPrompt, sysPrompt)
                
                val actionObj = parseAction(actionJsonStr)
                val action = actionObj.optString("action", "done")

                when (action) {
                    "tap" -> {
                        val x = actionObj.optDouble("x", 540.0).toFloat()
                        val y = actionObj.optDouble("y", 960.0).toFloat()
                        onProgress("Tapping at ($x, $y)")
                        NovaAccessibilityService.tap(x, y)
                        delay(1200)
                    }
                    "swipe" -> {
                        val sx = actionObj.optDouble("startX", 500.0).toFloat()
                        val sy = actionObj.optDouble("startY", 1200.0).toFloat()
                        val ex = actionObj.optDouble("endX", 500.0).toFloat()
                        val ey = actionObj.optDouble("endY", 400.0).toFloat()
                        onProgress("Swiping from ($sx, $sy) to ($ex, $ey)")
                        NovaAccessibilityService.swipe(sx, sy, ex, ey)
                        delay(1200)
                    }
                    "type" -> {
                        val txt = actionObj.optString("text", "")
                        onProgress("Typing: $txt")
                        NovaAccessibilityService.typeText(txt)
                        delay(1000)
                    }
                    "done" -> {
                        val summary = actionObj.optString("summary", "Goal completed successfully.")
                        return AgentResult(true, summary)
                    }
                }
            }

            return AgentResult(true, "Completed maximum steps ($MAX_STEPS) for goal: $goal")
        } catch (e: Exception) {
            return AgentResult(false, "Agent execution error: ${e.message}")
        } finally {
            isRunning = false
        }
    }

    private fun isPaymentScreen(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("enter pin") ||
               lower.contains("cvv") ||
               lower.contains("credit card number") ||
               lower.contains("cardholder name") ||
               lower.contains("pay now") ||
               lower.contains("authorize payment") ||
               lower.contains("confirm payment") ||
               lower.contains("upi pin")
    }

    private fun parseAction(response: String): JSONObject {
        return try {
            val jsonStart = response.indexOf("{")
            val jsonEnd = response.lastIndexOf("}")
            if (jsonStart != -1 && jsonEnd != -1) {
                JSONObject(response.substring(jsonStart, jsonEnd + 1))
            } else {
                JSONObject().put("action", "done").put("summary", "Goal concluded.")
            }
        } catch (e: Exception) {
            JSONObject().put("action", "done").put("summary", "Concluded.")
        }
    }
}

data class AgentResult(
    val success: Boolean,
    val message: String
)
