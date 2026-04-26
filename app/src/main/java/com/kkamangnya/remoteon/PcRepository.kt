package com.kkamangnya.remoteon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class PcRepository(private val store: PcStore) {
    suspend fun loadPcs(): List<RemotePc> = withContext(Dispatchers.IO) {
        store.load()
    }

    suspend fun upsertPc(pc: RemotePc) = withContext(Dispatchers.IO) {
        val current = store.load().toMutableList()
        val normalized = pc.copy(
            id = if (pc.id.isBlank()) UUID.randomUUID().toString() else pc.id,
            connectionState = ConnectionState.Unknown,
            lastCheckedAt = null
        )
        val index = current.indexOfFirst { it.id == normalized.id }
        if (index >= 0) {
            current[index] = normalized
        } else {
            current.add(normalized)
        }
        store.save(current)
    }

    suspend fun deletePc(pcId: String) = withContext(Dispatchers.IO) {
        store.save(store.load().filterNot { it.id == pcId })
    }

    suspend fun sendWake(pc: RemotePc) = WakeOnLanSender.sendWakePacket(
        broadcastAddress = pc.broadcastAddress,
        macAddress = pc.macAddress
    )

    suspend fun checkOnline(pc: RemotePc): HostStatusResult {
        return HostStatusChecker.check(pc.ipAddress)
    }
}
