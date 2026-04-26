package com.kkamangnya.remoteon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

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
    private val DEFAULT_PROBE_PORTS = intArrayOf(3389, 445, 135, 80, 22)
    private const val CONNECT_TIMEOUT_MS = 800
    private const val PING_TIMEOUT_MS = 1800L

    suspend fun isOnline(ipAddress: String): Boolean = withContext(Dispatchers.IO) {
        if (tryTcpProbe(ipAddress)) return@withContext true
        if (tryIcmpPing(ipAddress)) return@withContext true
        false
    }

    private fun tryTcpProbe(ipAddress: String): Boolean {
        for (port in DEFAULT_PROBE_PORTS) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ipAddress, port), CONNECT_TIMEOUT_MS)
                }
                return true
            } catch (_: IOException) {
                // Try the next common Windows/LAN service port.
            }
        }
        return false
    }

    private fun tryIcmpPing(ipAddress: String): Boolean {
        val commands = listOf(
            arrayOf("/system/bin/ping", "-c", "1", "-W", "1", ipAddress),
            arrayOf("ping", "-c", "1", "-W", "1", ipAddress),
            arrayOf("/system/bin/ping", "-c", "1", ipAddress),
            arrayOf("ping", "-c", "1", ipAddress)
        )

        for (command in commands) {
            try {
                val process = ProcessBuilder(*command)
                    .redirectErrorStream(true)
                    .start()
                val finished = process.waitFor(PING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    continue
                }
                if (process.exitValue() == 0) return true
            } catch (_: IOException) {
                // Command not available on this device, keep trying.
            } catch (_: SecurityException) {
                // Some Android builds restrict process execution, fall through.
            }
        }
        return false
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
