package com.example.kdelockunlock

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

    enum class Mode {
        BROADCAST_INTENT,
        DIRECT_SOCKET
    }

    suspend fun lock(context: Context, mode: Mode = Mode.BROADCAST_INTENT, pcIp: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            when (mode) {
                Mode.BROADCAST_INTENT -> sendBroadcastLock(context, lock = true)
                Mode.DIRECT_SOCKET -> sendSocketLockPacket(pcIp, lock = true)
            }
        }
    }

    suspend fun unlock(context: Context, mode: Mode = Mode.BROADCAST_INTENT, pcIp: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            when (mode) {
                Mode.BROADCAST_INTENT -> sendBroadcastLock(context, lock = false)
                Mode.DIRECT_SOCKET -> sendSocketLockPacket(pcIp, lock = false)
            }
        }
    }

    private fun sendBroadcastLock(context: Context, lock: Boolean): Boolean {
        return try {
            val action = if (lock) {
                "org.kde.kdeconnect.action.LOCK"
            } else {
                "org.kde.kdeconnect.action.UNLOCK"
            }

            val intent = Intent(action).apply {
                setPackage(KDE_CONNECT_PACKAGE)
                putExtra("isLocked", lock)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }

            context.sendBroadcast(intent)
            Log.d(TAG, "Broadcast $action sent to KDE Connect.")

            // Also broadcast a generic plugin command as secondary attempt
            val genericIntent = Intent("org.kde.kdeconnect.Plugins.LockDevicePlugin.LOCK").apply {
                putExtra("isLocked", lock)
            }
            context.sendBroadcast(genericIntent)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending broadcast intent to KDE Connect", e)
            false
        }
    }

    private fun sendSocketLockPacket(pcIp: String?, lock: Boolean): Boolean {
        if (pcIp.isNull_or_empty()) {
            Log.e(TAG, "No PC IP address provided for direct socket mode.")
            return false
        }

        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(pcIp, KDE_CONNECT_PORT), 3000)

                val bodyObj = JSONObject().apply {
                    put("isLocked", lock)
                }

                val packetObj = JSONObject().apply {
                    put("id", System.currentTimeMillis())
                    put("type", "kdeconnect.lockdevice")
                    put("body", bodyObj)
                }

                val jsonStr = packetObj.toString() + "\n"
                val writer = OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                writer.write(jsonStr)
                writer.flush()
                Log.d(TAG, "Sent KDE Connect socket lock packet ($lock) to $pcIp:$KDE_CONNECT_PORT")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending socket packet to $pcIp", e)
            false
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
