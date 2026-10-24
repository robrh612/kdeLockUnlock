package com.example.kdelockunlock

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneWearableListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val nodeId = messageEvent.sourceNodeId
        Log.d(TAG, "Message received from watch: path=$path, nodeId=$nodeId")

        scope.launch {
            when (path) {
                "/kde/lock" -> {
                    val success = KdeConnectManager.lock(applicationContext)
                    replyToWatch(nodeId, "/kde/response", if (success) "LOCKED" else "ERROR")
                }
                "/kde/unlock" -> {
                    val success = KdeConnectManager.unlock(applicationContext)
                    replyToWatch(nodeId, "/kde/response", if (success) "UNLOCKED" else "ERROR")
                }
                "/kde/ping" -> {
                    replyToWatch(nodeId, "/kde/response", "PONG")
                }
                else -> {
                    Log.w(TAG, "Unknown message path: $path")
                }
            }
        }
    }

    private fun replyToWatch(nodeId: String, path: String, message: String) {
        try {
            Wearable.getMessageClient(this)
                .sendMessage(nodeId, path, message.toByteArray(Charsets.UTF_8))
            Log.d(TAG, "Replied to watch ($nodeId): $path -> $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send response back to watch", e)
        }
    }

    companion object {
        private const val TAG = "PhoneWearableListener"
    }
}
