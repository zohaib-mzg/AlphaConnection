package com.alphaconnection.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class NearbyManager(private val context: Context) {

    companion object {
        private const val TAG = "NearbyManager"
        const val SERVICE_ID = "com.alphaconnection.poc"
        private val STRATEGY = Strategy.P2P_STAR
        const val PAYLOAD_SEPARATOR = "|"
    }

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val connectedEndpoints = mutableMapOf<String, String>()
    private val pendingEndpoints = mutableSetOf<String>()

    val deviceName: String = Build.MODEL

    var onMessageReceived: ((sender: String, data: String) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null

    private var advertisingActive = false
    private var discoveryActive = false

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            log("Device Found: ${info.endpointName} ($endpointId)")
            onLog?.invoke("Device Found: ${info.endpointName}")

            if (connectedEndpoints.containsKey(endpointId) || pendingEndpoints.contains(endpointId)) {
                log("Skipping $endpointId - already connected or pending")
                return
            }

            pendingEndpoints.add(endpointId)
            log("Connection Requested: ${info.endpointName} ($endpointId)")
            onLog?.invoke("Connection Requested: ${info.endpointName}")

            try {
                connectionsClient.requestConnection(
                    deviceName, endpointId, connectionLifecycleCallback
                ).addOnSuccessListener {
                    log("Connection request sent to ${info.endpointName}")
                }.addOnFailureListener { e ->
                    pendingEndpoints.remove(endpointId)
                    log("Connection Failed: ${e.message} (${info.endpointName})")
                    onLog?.invoke("Connection Failed: ${e.message}")
                }
            } catch (e: Exception) {
                pendingEndpoints.remove(endpointId)
                log("Exception requesting connection: ${e.message}")
                onLog?.invoke("Connection error: ${e.message}")
            }
        }

        override fun onEndpointLost(endpointId: String) {
            pendingEndpoints.remove(endpointId)
            log("Device Lost: $endpointId")
            onLog?.invoke("Device Lost")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            log("Connection Accepted: ${connectionInfo.endpointName}")
            onLog?.invoke("Accepting connection: ${connectionInfo.endpointName}")

            try {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            } catch (e: Exception) {
                log("Exception accepting connection: ${e.message}")
                onLog?.invoke("Accept error: ${e.message}")
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            pendingEndpoints.remove(endpointId)

            if (result.status.isSuccess) {
                connectedEndpoints[endpointId] = result.status.statusMessage ?: endpointId
                log("Connected: $endpointId (${connectedEndpoints.size} device(s))")
                onLog?.invoke("Connected to device")
                updateStatus()
            } else {
                log("Connection Failed: $endpointId - ${result.status.statusMessage}")
                onLog?.invoke("Connection failed")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            pendingEndpoints.remove(endpointId)
            log("Disconnected: $endpointId (${connectedEndpoints.size} device(s))")
            onLog?.invoke("Disconnected from device")
            updateStatus()

            if (discoveryActive) {
                restartDiscovery()
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val raw = String(bytes)
                val separatorIndex = raw.indexOf(PAYLOAD_SEPARATOR)

                if (separatorIndex > 0) {
                    val sender = raw.substring(0, separatorIndex)
                    val data = raw.substring(separatorIndex + 1)
                    log("Payload Received: [$sender] $data")
                    onMessageReceived?.invoke(sender, data)
                } else {
                    log("Payload Received: [Unknown] $raw")
                    onMessageReceived?.invoke("Unknown", raw)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Transfer progress tracking (optional for PoC)
        }
    }

    fun initialize() {
        log("Initializing...")
        onLog?.invoke("Initializing...")

        try {
            startAdvertising()
            startDiscovery()
        } catch (e: Exception) {
            log("Exception during initialization: ${e.message}")
            onLog?.invoke("Init error: ${e.message}")
        }
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        try {
            connectionsClient.startAdvertising(
                deviceName, SERVICE_ID, connectionLifecycleCallback, options
            ).addOnSuccessListener {
                advertisingActive = true
                log("Advertising Started: $deviceName")
                onLog?.invoke("Advertising Started")
                onStatusChanged?.invoke("Advertising...")
            }.addOnFailureListener { e ->
                advertisingActive = false
                log("Advertising Failed: ${e.message}")
                onLog?.invoke("Advertising Failed: ${e.message}")
            }
        } catch (e: Exception) {
            log("Exception starting advertising: ${e.message}")
            onLog?.invoke("Advertising error: ${e.message}")
        }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        try {
            connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnSuccessListener {
                    discoveryActive = true
                    log("Discovery Started")
                    onLog?.invoke("Discovery Started")
                    if (connectedEndpoints.isEmpty()) {
                        onStatusChanged?.invoke("Discovering...")
                    }
                }.addOnFailureListener { e ->
                    discoveryActive = false
                    log("Discovery Failed: ${e.message}")
                    onLog?.invoke("Discovery Failed: ${e.message}")
                }
        } catch (e: Exception) {
            log("Exception starting discovery: ${e.message}")
            onLog?.invoke("Discovery error: ${e.message}")
        }
    }

    private fun restartDiscovery() {
        try {
            connectionsClient.stopDiscovery()
            startDiscovery()
        } catch (e: Exception) {
            log("Exception restarting discovery: ${e.message}")
        }
    }

    fun broadcast(data: String) {
        if (connectedEndpoints.isEmpty()) {
            log("No connected devices to broadcast to")
            onLog?.invoke("No connected devices")
            return
        }

        val payloadData = "$deviceName$PAYLOAD_SEPARATOR$data"
        val payload = Payload.fromBytes(payloadData.toByteArray())
        val endpointIds = connectedEndpoints.keys.toList()

        try {
            connectionsClient.sendPayload(endpointIds, payload)
                .addOnSuccessListener {
                    log("Broadcast Sent to ${endpointIds.size} device(s): $data")
                    onLog?.invoke("Broadcast Sent: $data")
                }.addOnFailureListener { e ->
                    log("Broadcast Failed: ${e.message}")
                    onLog?.invoke("Send Failed: ${e.message}")
                }
        } catch (e: Exception) {
            log("Exception sending payload: ${e.message}")
            onLog?.invoke("Send error: ${e.message}")
        }
    }

    fun shutdown() {
        log("Shutting down...")
        onLog?.invoke("Shutting Down...")

        try {
            connectionsClient.stopAdvertising()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping advertising: ${e.message}")
        }

        try {
            connectionsClient.stopDiscovery()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping discovery: ${e.message}")
        }

        try {
            connectionsClient.stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping endpoints: ${e.message}")
        }

        connectedEndpoints.clear()
        pendingEndpoints.clear()
        advertisingActive = false
        discoveryActive = false

        onStatusChanged?.invoke("Disconnected")
        onLog?.invoke("Shutdown Complete")
        log("Shutdown Complete")
    }

    fun isConnected(): Boolean = connectedEndpoints.isNotEmpty()

    fun getConnectedCount(): Int = connectedEndpoints.size

    private fun updateStatus() {
        val count = connectedEndpoints.size
        val status = when (count) {
            0 -> "Disconnected"
            1 -> "Connected (1 Device)"
            else -> "Connected ($count Devices)"
        }
        onStatusChanged?.invoke(status)
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }
}
