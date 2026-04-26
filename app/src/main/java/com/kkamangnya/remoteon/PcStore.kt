package com.kkamangnya.remoteon

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PcStore(
    context: Context,
    private val crypto: PrefsCrypto = PrefsCrypto()
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): List<RemotePc> {
        val stored = preferences.getString(KEY_PCS, null) ?: return emptyList()
        val json = runCatching { crypto.decrypt(stored) }.getOrNull() ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        RemotePc(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            macAddress = item.getString("macAddress"),
                            ipAddress = item.getString("ipAddress"),
                            subnetMask = item.optString("subnetMask", ""),
                            broadcastAddress = item.getString("broadcastAddress")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(pcs: List<RemotePc>) {
        val json = JSONArray().apply {
            pcs.forEach { pc ->
                put(
                    JSONObject()
                        .put("id", pc.id)
                        .put("name", pc.name)
                        .put("macAddress", pc.macAddress)
                        .put("ipAddress", pc.ipAddress)
                        .put("subnetMask", pc.subnetMask)
                        .put("broadcastAddress", pc.broadcastAddress)
                )
            }
        }
        preferences.edit()
            .putString(KEY_PCS, crypto.encrypt(json.toString()))
            .apply()
    }

    companion object {
        private const val FILE_NAME = "remoteon_prefs"
        private const val KEY_PCS = "pcs"
    }
}
