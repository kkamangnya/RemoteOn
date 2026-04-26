package com.kkamangnya.remoteon

import java.util.UUID

enum class ConnectionState {
    Unknown,
    Online,
    Offline
}

data class RemotePc(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val macAddress: String,
    val ipAddress: String,
    val subnetMask: String = "",
    val broadcastAddress: String,
    val connectionState: ConnectionState = ConnectionState.Unknown,
    val lastCheckedAt: Long? = null
)
