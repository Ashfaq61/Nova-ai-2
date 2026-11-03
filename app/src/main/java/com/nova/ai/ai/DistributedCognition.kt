package com.nova.ai.ai

import org.json.JSONArray
import org.json.JSONObject

object DistributedCognition {
    data class PeerNode(val nodeId: String, val role: String, val batteryLevel: Int, val isAvailable: Boolean)

    private val peerRegistry = mutableListOf<PeerNode>()

    fun registerPeer(node: PeerNode) {
        peerRegistry.removeAll { it.nodeId == node.nodeId }
        peerRegistry.add(node)
    }

    fun getActivePeers(): List<PeerNode> = peerRegistry.filter { it.isAvailable }

    fun synthesizeConsensus(query: String, responses: List<String>): String {
        if (responses.isEmpty()) return "No responses received for consensus."
        return "Consensus synthesis across ${responses.size} nodes: " + responses.joinToString(" | ")
    }

    fun getClusterStatus(): String {
        val array = JSONArray()
        peerRegistry.forEach { peer ->
            array.put(JSONObject().apply {
                put("id", peer.nodeId)
                put("role", peer.role)
                put("battery", peer.batteryLevel)
            })
        }
        return JSONObject().put("peers", array).put("count", peerRegistry.size).toString()
    }
}
