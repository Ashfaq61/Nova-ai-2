package com.example.ar

import org.json.JSONArray
import org.json.JSONObject

data class DetectedObject(
    val id: String,
    val label: String,
    val distanceMeters: Float,
    val directionAngleDeg: Float,
    val confidence: Float,
    val lastSeenRoom: String
)

class ArOverlayEngine {
    private var isArActive = false
    private val spatialMemory = mutableListOf(
        DetectedObject("obj_1", "Car Keys", 1.8f, 32f, 0.94f, "Living Room Table"),
        DetectedObject("obj_2", "MacBook Pro", 0.9f, -12f, 0.98f, "Work Desk"),
        DetectedObject("obj_3", "AirPods Case", 2.4f, 75f, 0.88f, "Bedside Stand"),
        DetectedObject("obj_4", "Passport", 3.1f, -45f, 0.91f, "Top Drawer")
    )

    fun toggleAR(enabled: Boolean): Boolean {
        isArActive = enabled
        return isArActive
    }

    fun isEnabled(): Boolean = isArActive

    fun searchObject(query: String): String {
        val q = query.lowercase()
        val match = spatialMemory.firstOrNull { it.label.lowercase().contains(q) }
        return if (match != null) {
            val arrowDirection = if (match.directionAngleDeg > 0) "right (${match.directionAngleDeg.toInt()}°)" else "left (${Math.abs(match.directionAngleDeg).toInt()}°)"
            JSONObject().apply {
                put("found", true)
                put("label", match.label)
                put("location", match.lastSeenRoom)
                put("distance", String.format("%.1fm", match.distanceMeters))
                put("guidance", "Found ${match.label} at ${match.lastSeenRoom}, approx ${String.format("%.1fm", match.distanceMeters)} towards your $arrowDirection.")
                put("arrowAngle", match.directionAngleDeg)
                put("confidence", match.confidence)
            }.toString()
        } else {
            JSONObject().apply {
                put("found", false)
                put("query", query)
                put("guidance", "Scanning AR scene... Object '$query' not yet observed in active spatial memory.")
            }.toString()
        }
    }

    fun getDetectedObjects(): String {
        val array = JSONArray()
        for (item in spatialMemory) {
            array.put(JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("distance", item.distanceMeters)
                put("angle", item.directionAngleDeg)
                put("location", item.lastSeenRoom)
            })
        }
        return array.toString()
    }
}
