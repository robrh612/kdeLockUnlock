package com.example.kdelockunlock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import org.json.JSONObject

object KdeConnectManager {
    private const val TAG = "KdeConnectManager"
    private const val KDE_CONNECT_PACKAGE = "org.kde.kdeconnect_tp"
    private const val KDE_CONNECT_PORT = 1716
    private const val PREFS_NAME = "kde_settings"
    private const val PREF_KEY_PC_IP = "pc_ip"

    fun getSavedPcIp(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY_PC_IP, "") ?: ""
    }

    fun savePcIp(context: Context, ip: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_PC_IP, ip.trim())
            .apply()
    }

    suspend fun lock(context: Context, pcIp: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            val targetIp = if (!pcIp.isNullOrBlank()) pcIp else getSavedPcIp(context)
            val successIntent = sendBroadcastLock(context, lock = true)
            var successSocket = false
            if (targetIp.isNotBlank()) {
                successSocket = sendSocketLockPacket(targetIp, lock = true)
            }
            Log.d(TAG, "Lock command completed. IntentSent=$successIntent, SocketSent=$successSocket")
            successIntent || successSocket
        }
    }

    suspend fun unlock(context: Context, pcIp: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            val targetIp = if (!pcIp.isNullOrBlank()) pcIp else getSavedPcIp(context)
            val successIntent = sendBroadcastLock(context, lock = false)
            var successSocket = false
            if (targetIp.isNotBlank()) {
                successSocket = sendSocketLockPacket(targetIp, lock = false)
            }
            Log.d(TAG, "Unlock command completed. IntentSent=$successIntent, SocketSent=$successSocket")
            successIntent || successSocket
        }
    }

    private fun sendBroadcastLock(context: Context, lock: Boolean): Boolean {
        var dispatchedCount = 0

        val actions = if (lock) {
            listOf(
                "org.kde.kdeconnect.action.LOCK",
                "org.kde.kdeconnect.LOCK",
                "org.kde.kdeconnect.Plugins.LockDevicePlugin.LOCK",
                "org.kde.kdeconnect_tp.LOCK"
            )
        } else {
            listOf(
                "org.kde.kdeconnect.action.UNLOCK",
                "org.kde.kdeconnect.UNLOCK",
                "org.kde.kdeconnect.Plugins.LockDevicePlugin.UNLOCK",
                "org.kde.kdeconnect_tp.UNLOCK"
            )
        }

        for (actionStr in actions) {
            try {
                val intent = Intent(actionStr).apply {
                    setPackage(KDE_CONNECT_PACKAGE)
                    putExtra("isLocked", lock)
                    putExtra("setLocked", lock)
                    putExtra("action", if (lock) "lock" else "unlock")
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                }
                context.sendBroadcast(intent)
                dispatchedCount++
            } catch (e: Exception) {
                Log.w(TAG, "Failed sending broadcast action: $actionStr", e)
            }
        }

        // Also attempt sending explicit intent to KDE Connect's background components
        val explicitComponents = listOf(
            "org.kde.kdeconnect.UserInterface.Receiver",
            "org.kde.kdeconnect.Plugins.LockDevicePlugin.LockDevicePlugin",
            "org.kde.kdeconnect.BackgroundService"
        )

        for (comp in explicitComponents) {
            try {
                val explicitIntent = Intent(if (lock) "org.kde.kdeconnect.action.LOCK" else "org.kde.kdeconnect.action.UNLOCK").apply {
                    component = ComponentName(KDE_CONNECT_PACKAGE, comp)
                    putExtra("isLocked", lock)
                    putExtra("setLocked", lock)
                }
                context.sendBroadcast(explicitIntent)
                dispatchedCount++
            } catch (_: Exception) {
                // Ignore if component not present in installed version
            }
        }

        return dispatchedCount > 0
    }

    private fun sendSocketLockPacket(pcIp: String, lock: Boolean): Boolean {
        if (pcIp.isBlank()) return false

        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(pcIp, KDE_CONNECT_PORT), 3000)

                val bodyObj = JSONObject().apply {
                    put("setLocked", lock)
                    put("isLocked", lock)
                }

                val packetObj1 = JSONObject().apply {
                    put("id", System.currentTimeMillis())
                    put("type", "kdeconnect.lockdevice.request")
                    put("body", bodyObj)
                }

                val packetObj2 = JSONObject().apply {
                    put("id", System.currentTimeMillis() + 1)
                    put("type", "kdeconnect.lockdevice")
                    put("body", bodyObj)
                }

                val writer = OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                writer.write(packetObj1.toString() + "\n")
                writer.write(packetObj2.toString() + "\n")
                writer.flush()
                Log.d(TAG, "Sent KDE Connect socket lock packet ($lock) to $pcIp:$KDE_CONNECT_PORT")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending direct socket packet to $pcIp", e)
            false
        }
    }
}
