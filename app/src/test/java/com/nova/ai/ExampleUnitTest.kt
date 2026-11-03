package com.nova.ai

import com.nova.ai.utils.PrivacyScrubber
import org.junit.Test
import org.junit.Assert.*

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
}
