package com.nova.ai.utils

import org.json.JSONArray
import org.json.JSONObject

object KnowledgeGraph {
    data class MemoryNode(val subject: String, val predicate: String, val obj: String, val confidence: Double)

    private val nodes = mutableListOf<MemoryNode>()

    fun addFact(subject: String, predicate: String, obj: String, confidence: Double = 1.0) {
        nodes.add(MemoryNode(subject, predicate, obj, confidence))
    }

    fun queryFacts(subject: String): List<MemoryNode> {
        return nodes.filter { it.subject.equals(subject, ignoreCase = true) }
    }

    fun dumpGraphJson(): String {
        val array = JSONArray()
        nodes.forEach { node ->
            array.put(JSONObject().apply {
                put("s", node.subject)
                put("p", node.predicate)
                put("o", node.obj)
                put("c", node.confidence)
            })
        }
        return JSONObject().put("facts", array).toString()
    }
}
