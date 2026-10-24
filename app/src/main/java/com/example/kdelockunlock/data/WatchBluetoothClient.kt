package com.example.kdelockunlock.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

class WatchBluetoothClient(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _statusText = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val prefs = context.getSharedPreferences("bt_settings", Context.MODE_PRIVATE)

    fun getSavedMac(): String {
        val saved = prefs.getString(PREF_MAC, "") ?: ""
        if (saved.isNotBlank()) return saved
        // TODO: Replace with your Debian PC's Bluetooth MAC address
        return ""
    }

    fun saveMac(mac: String) {
        prefs.edit().putString(PREF_MAC, mac.trim().uppercase()).apply()
        _statusText.value = "MAC Saved"
    }

    suspend fun sendLockCommand() {
        sendCommand("LOCK", "PC Locked!")
    }

    suspend fun sendUnlockCommand() {
        sendCommand("UNLOCK", "PC Unlocked!")
    }

    @SuppressLint("MissingPermission", "DiscouragedPrivateApi")
    private suspend fun sendCommand(command: String, successMsg: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                _statusText.value = "Grant BT Perm!"
                return
            }
        }

        val mac = getSavedMac()
        if (mac.isBlank()) {
            _statusText.value = "Set PC BT MAC!"
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _statusText.value = "Bluetooth Off"
            return
        }

        withContext(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                _statusText.value = "Connecting..."
                val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(mac)
                
                // Try createRfcommSocket(1) via reflection, fallback to createInsecureRfcommSocket(1) or UUID
                val socketToUse = try {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    m.invoke(device, 1) as BluetoothSocket
                } catch (_: Exception) {
                    try {
                        val m2 = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                        m2.invoke(device, 1) as BluetoothSocket
                    } catch (_: Exception) {
                        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        device.createRfcommSocketToServiceRecord(uuid)
                    }
                }
                
                socket = socketToUse
                socket.connect()
                _isConnected.value = true
                _statusText.value = "Sending..."

                val outputStream: OutputStream = socket.outputStream
                outputStream.write("$command\n".toByteArray(Charsets.UTF_8))
                outputStream.flush()

                Thread.sleep(100)
                _statusText.value = successMsg
            } catch (e: Exception) {
                Log.e(TAG, "Bluetooth connection/command error", e)
                _isConnected.value = false
                val err = e.localizedMessage ?: "Unknown error"
                _statusText.value = when {
                    err.contains("permission", true) -> "Need BT Perm"
                    err.contains("refused", true) -> "Connection Refused"
                    err.contains("timeout", true) -> "Connection Timeout"
                    else -> "Connect Failed"
                }
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        private const val TAG = "WatchBluetoothClient"
        private const val PREF_MAC = "pc_bluetooth_mac"
    }
}
