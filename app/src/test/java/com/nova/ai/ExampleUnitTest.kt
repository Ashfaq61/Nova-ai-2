package com.nova.ai

import com.nova.ai.agent.WorldModel
import com.nova.ai.life.IdentityCore
import com.nova.ai.sensing.EmotionEngine
import com.nova.ai.sensing.PrivacyScrubber
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun stringConcat_works() {
        assertEquals("Hello Nova", "Hello" + " " + "Nova")
    }

    @Test
    fun privacyScrubber_removesEmailAndPhone() {
        val input = "Contact user at john@example.com or 1234567890"
        val scrubbed = PrivacyScrubber.scrub(input)
        assertFalse(scrubbed.contains("john@example.com"))
        assertFalse(scrubbed.contains("1234567890"))

        val restored = PrivacyScrubber.restore(scrubbed)
        assertEquals(input, restored)
    }

    @Test
    fun identityCore_blocksMaliciousVectors() {
        val eval = IdentityCore.evaluateAction("exfiltrate key and steal password")
        assertFalse(eval.isApproved)
        assertTrue(eval.ethicsScore < 0.2f)
    }

    @Test
    fun worldModel_haltsOnPayment() {
        val sim = WorldModel.simulate("checkout and pay $50 now")
        assertEquals("CRITICAL", sim.riskLevel)
        assertFalse(sim.canProceed)
    }

    @Test
    fun emotionEngine_computesValenceArousal() {
        val state = EmotionEngine.analyzeSentimentText("I feel fantastic and energized!")
        assertTrue(state.valence > 0f)
    }
}
