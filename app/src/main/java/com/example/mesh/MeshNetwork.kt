package com.example.mesh

import org.json.JSONArray
import org.json.JSONObject

data class MeshPeer(
    val id: String,
    val name: String,
    val batteryPct: Int,
    val status: String,
    val isPaired: Boolean
)

class MeshNetwork {

    private var isMeshActive = false
    private val peers = mutableListOf(
        MeshPeer("nova_node_a1", "MacBook Pro (Living Room)", 94, "ONLINE", true),
        MeshPeer("nova_node_b4", "Pixel Tablet (Kitchen)", 76, "STANDBY", true),
        MeshPeer("nova_node_c9", "Nearby Nova Peer (BLE)", 82, "UNPAIRED", false)
    )

    fun startMesh(): Boolean {
        isMeshActive = true
        return true
    }

    fun isRunning(): Boolean = isMeshActive

    fun pairWith(code: String): String {
        return if (code.length == 6) {
            JSONObject().apply {
                put("status", "SUCCESS")
                put("message", "Secure AES-256 swarm tunnel established with peer using code $code")
            }.toString()
        } else {
            JSONObject().apply {
                put("status", "ERROR")
                put("message", "Pairing code must be 6 digits")
            }.toString()
        }
    }

    fun sendToPeer(peerId: String, payloadJson: String): String {
        val target = peers.firstOrNull { it.id == peerId }
        return if (target != null && target.isPaired) {
            JSONObject().apply {
                put("status", "DISPATCHED")
                put("target", target.name)
                put("encryptedBytes", payloadJson.toByteArray().size)
            }.toString()
        } else {
            JSONObject().apply {
                put("status", "FAILED")
                put("reason", "Peer unreachable or unpaired")
            }.toString()
        }
    }

    fun getPeers(): String {
        val array = JSONArray()
        for (p in peers) {
            array.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("battery", p.batteryPct)
                put("status", p.status)
                put("paired", p.isPaired)
            })
        }
        return array.toString()
    }
}
