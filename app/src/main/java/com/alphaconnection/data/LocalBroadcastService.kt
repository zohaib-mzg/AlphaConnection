package com.alphaconnection.data

import android.app.Activity

class LocalBroadcastService(private val activity: Activity) {

    private val nearbyManager = NearbyManager(activity)

    var onReceiveCallback: ((sender: String, data: String) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null
    var onLog: ((String) -> Unit)? = null

    private var initialized = false

    val deviceName: String get() = nearbyManager.deviceName

    fun initialize() {
        if (initialized) {
            onLog?.invoke("Already Initialized - ignoring duplicate call")
            return
        }

        nearbyManager.onMessageReceived = { sender, data ->
            onReceive(sender, data)
        }
        nearbyManager.onStatusChanged = { status ->
            onStatusChanged?.invoke(status)
        }
        nearbyManager.onLog = { log ->
            onLog?.invoke(log)
        }

        nearbyManager.initialize()
        initialized = true
    }

    fun broadcast(data: String) {
        if (!initialized) {
            onLog?.invoke("Not initialized. Call initialize() first.")
            return
        }
        nearbyManager.broadcast(data)
    }

    fun onReceive(sender: String, data: String) {
        onReceiveCallback?.invoke(sender, data)
    }

    fun shutdown() {
        if (!initialized) {
            onLog?.invoke("Not initialized.")
            return
        }

        nearbyManager.shutdown()
        initialized = false
    }

    fun isConnected(): Boolean = nearbyManager.isConnected()

    fun getConnectedCount(): Int = nearbyManager.getConnectedCount()
}
