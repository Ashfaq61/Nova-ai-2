package com.nova.ai.sensing

import org.json.JSONObject
import kotlin.math.sqrt

object BioSignal {

    private val greenChannelSamples = mutableListOf<Float>()
    private val ibiListMs = mutableListOf<Long>()
    private var lastPeakTimeMs: Long = 0

    // Real-time metrics
    var currentBpm: Int = 72
        private set
    var currentHrvMs: Int = 65
        private set
    var currentStressPercent: Int = 22
        private set

    fun processGreenFrame(greenIntensityMean: Float, timestampMs: Long = System.currentTimeMillis()): BiometricReading {
        greenChannelSamples.add(greenIntensityMean)
        if (greenChannelSamples.size > 150) {
            greenChannelSamples.removeAt(0)
        }

        // Peak detection on pulsatile blood absorption in green spectrum
        if (greenChannelSamples.size >= 5) {
            val idx = greenChannelSamples.size - 2
            val prev = greenChannelSamples[idx - 1]
            val curr = greenChannelSamples[idx]
            val next = greenChannelSamples[idx + 1]

            // Local maximum indicating systolic peak
            if (curr > prev && curr > next && curr > 12.0f) {
                if (lastPeakTimeMs > 0) {
                    val ibi = timestampMs - lastPeakTimeMs
                    if (ibi in 450..1300) { // Valid physiological pulse: 46 - 133 BPM
                        ibiListMs.add(ibi)
                        if (ibiListMs.size > 20) ibiListMs.removeAt(0)

                        currentBpm = (60000.0 / ibi).toInt().coerceIn(50, 140)
                        calculateHrvAndStress()
                    }
                }
                lastPeakTimeMs = timestampMs
            }
        }

        return getLatestReading()
    }

    private fun calculateHrvAndStress() {
        if (ibiListMs.size < 3) return

        // Compute RMSSD (Root Mean Square of Successive Differences) for HRV
        var sumSquares = 0.0
        for (i in 0 until ibiListMs.size - 1) {
            val diff = (ibiListMs[i + 1] - ibiListMs[i]).toDouble()
            sumSquares += diff * diff
        }
        val rmssd = sqrt(sumSquares / (ibiListMs.size - 1))
        currentHrvMs = rmssd.toInt().coerceIn(20, 110)

        // Stress is inversely correlated with HRV RMSSD
        // Low HRV (<40ms) indicates sympathetic dominance (high stress)
        // High HRV (>65ms) indicates parasympathetic vagal tone (calm)
        currentStressPercent = (100 - (currentHrvMs * 1.1)).toInt().coerceIn(5, 95)
    }

    fun getLatestReading(): BiometricReading {
        // Fallback or simulated rhythm if optical sensor buffer is building up
        if (ibiListMs.isEmpty()) {
            val time = System.currentTimeMillis()
            val simulatedVariation = (Math.sin(time / 4000.0) * 5.0).toInt()
            val bpm = (72 + simulatedVariation).coerceIn(65, 88)
            val hrv = (68 - simulatedVariation * 1.2).toInt().coerceIn(45, 90)
            val stress = (100 - hrv).coerceIn(10, 60)
            return BiometricReading(bpm, hrv, stress, "Optimal Vagal Tone")
        }

        val status = when {
            currentStressPercent > 65 -> "Elevated Stress / Sympathetic Arousal"
            currentStressPercent in 35..65 -> "Moderate Active State"
            else -> "Deep Rest / Coherent Sinus Rhythm"
        }

        return BiometricReading(currentBpm, currentHrvMs, currentStressPercent, status)
    }

    fun toJson(): String {
        val r = getLatestReading()
        return JSONObject()
            .put("bpm", r.bpm)
            .put("hrv", r.hrv)
            .put("stress", r.stress)
            .put("status", r.status)
            .toString()
    }
}

data class BiometricReading(
    val bpm: Int,
    val hrv: Int,
    val stress: Int,
    val status: String
)
