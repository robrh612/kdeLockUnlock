package com.example.kdelockunlock.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchWearableClient(private val context: Context) : MessageClient.OnMessageReceivedListener {

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _statusText = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun registerListener() {
        messageClient.addListener(this)
        checkConnection()
    }

    fun unregisterListener() {
        messageClient.removeListener(this)
    }

    fun checkConnection() {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                val connected = nodes.isNotEmpty()
                _isConnected.value = connected
                if (connected) {
                    _statusText.value = "Connected to Phone"
                } else {
                    _statusText.value = "Phone disconnected"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get connected nodes", e)
                _statusText.value = "Phone unreachable"
                _isConnected.value = false
            }
    }

    fun sendLockCommand(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        sendMessage("/kde/lock", "Lock", onResult)
    }

    fun sendUnlockCommand(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        sendMessage("/kde/unlock", "Unlock", onResult)
    }

    fun sendPing() {
        sendMessage("/kde/ping", "Ping") { success, _ ->
            if (success) {
                _statusText.value = "Phone connected"
            } else {
                _statusText.value = "Phone disconnected"
            }
        }
    }

    private fun sendMessage(path: String, actionName: String, onResult: (Boolean, String) -> Unit) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    _statusText.value = "No paired phone"
                    _isConnected.value = false
                    onResult(false, "No paired phone")
                    return@addOnSuccessListener
                }

                _statusText.value = "Sending $actionName..."
                _isConnected.value = true

                var count = 0
                val total = nodes.size
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, ByteArray(0))
                        .addOnSuccessListener {
                            count++
                            if (count == total) {
                                _statusText.value = "$actionName sent"
                                onResult(true, "Sent")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error sending to node ${node.id}", e)
                            _statusText.value = "Send failed"
                            onResult(false, e.localizedMessage ?: "Failed")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error discovering nodes", e)
                _statusText.value = "Phone disconnected"
                _isConnected.value = false
                onResult(false, e.localizedMessage ?: "Error")
            }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == "/kde/response") {
            val response = String(event.data, Charsets.UTF_8)
            Log.d(TAG, "Received response from phone: $response")
            when (response) {
                "LOCKED" -> _statusText.value = "PC Locked!"
                "UNLOCKED" -> _statusText.value = "PC Unlocked!"
                "PONG" -> _statusText.value = "Phone Online"
                "ERROR" -> _statusText.value = "KDE Connect Error"
                else -> _statusText.value = response
            }
        }
    }

    companion object {
        private const val TAG = "WatchWearableClient"
    }
}
