package com.kkamangnya.remoteon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object WakeOnLanSender {
    private const val WOL_PORT = 9
    private const val MAC_LENGTH_BYTES = 6
    private const val MAC_REPEAT_COUNT = 16

    suspend fun sendWakePacket(broadcastAddress: String, macAddress: String) = withContext(Dispatchers.IO) {
        val packetBytes = buildMagicPacket(macAddress)
        val address = InetAddress.getByName(broadcastAddress)

        DatagramSocket().use { socket ->
            socket.broadcast = true
            val packet = DatagramPacket(packetBytes, packetBytes.size, address, WOL_PORT)
            socket.send(packet)
        }
    }

    fun buildMagicPacket(macAddress: String): ByteArray {
        val normalized = normalizeMac(macAddress)
        require(normalized.length == MAC_LENGTH_BYTES * 2) {
            "MAC 주소는 12자리 16진수여야 합니다."
        }

        val payload = ByteArray(6 + MAC_LENGTH_BYTES * MAC_REPEAT_COUNT)
        repeat(6) { payload[it] = 0xFF.toByte() }

        val macBytes = normalized.chunked(2).map { it.toInt(16).toByte() }
        for (repeatIndex in 0 until MAC_REPEAT_COUNT) {
            val base = 6 + repeatIndex * MAC_LENGTH_BYTES
            macBytes.forEachIndexed { offset, byte ->
                payload[base + offset] = byte
            }
        }
        return payload
    }

    private fun normalizeMac(macAddress: String): String {
        return macAddress.replace(":", "")
            .replace("-", "")
            .replace(" ", "")
            .lowercase()
    }
}

object HostStatusChecker {
    private const val DEFAULT_PROBE_PORT = 3389
    private const val CONNECT_TIMEOUT_MS = 800

    suspend fun isOnline(ipAddress: String, port: Int = DEFAULT_PROBE_PORT): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), CONNECT_TIMEOUT_MS)
            }
            true
        } catch (_: IOException) {
            false
        }
    }
}

object NetworkTools {
    fun isValidIpv4(value: String): Boolean {
        val octets = value.split(".")
        if (octets.size != 4) return false
        return octets.all { part ->
            val number = part.toIntOrNull() ?: return false
            number in 0..255
        }
    }

    fun calculateBroadcastAddress(ipAddress: String, subnetMask: String): String? {
        if (!isValidIpv4(ipAddress) || !isValidIpv4(subnetMask)) return null
        val ipParts = ipAddress.split(".").map { it.toInt() }
        val maskParts = subnetMask.split(".").map { it.toInt() }

        val broadcastParts = ipParts.zip(maskParts).map { (ip, mask) ->
            (ip or mask.inv()) and 0xFF
        }

        return broadcastParts.joinToString(".")
    }
}
