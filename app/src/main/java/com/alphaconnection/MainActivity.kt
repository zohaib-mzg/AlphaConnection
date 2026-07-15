package com.alphaconnection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alphaconnection.data.LocalBroadcastService
import com.alphaconnection.data.MessageModel
import com.alphaconnection.ui.MessageAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var broadcastService: LocalBroadcastService
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: MutableList<MessageModel>

    private lateinit var tvDeviceName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnInitialize: MaterialButton
    private lateinit var btnShutdown: MaterialButton
    private lateinit var etMessage: EditText
    private lateinit var btnBroadcast: MaterialButton
    private lateinit var rvMessages: RecyclerView
    private lateinit var tvNoMessages: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var tabReceived: TextView
    private lateinit var tabLog: TextView
    private lateinit var frameMessages: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupBroadcastService()
        setupClickListeners()
        checkPermissions()
    }

    private fun initViews() {
        tvDeviceName = findViewById(R.id.tvDeviceName)
        tvStatus = findViewById(R.id.tvStatus)
        btnInitialize = findViewById(R.id.btnInitialize)
        btnShutdown = findViewById(R.id.btnShutdown)
        etMessage = findViewById(R.id.etMessage)
        btnBroadcast = findViewById(R.id.btnBroadcast)
        rvMessages = findViewById(R.id.rvMessages)
        tvNoMessages = findViewById(R.id.tvNoMessages)
        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)
        tabReceived = findViewById(R.id.tabReceived)
        tabLog = findViewById(R.id.tabLog)
        frameMessages = findViewById(R.id.frameMessages)

        btnShutdown.isEnabled = false
        btnBroadcast.isEnabled = false
    }

    private fun setupRecyclerView() {
        messageList = mutableListOf()
        messageAdapter = MessageAdapter(messageList)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        rvMessages.adapter = messageAdapter
    }

    private fun setupBroadcastService() {
        broadcastService = LocalBroadcastService(this)

        tvDeviceName.text = "Device: ${broadcastService.deviceName}"

        broadcastService.onReceiveCallback = { sender, data ->
            runOnUiThread {
                addMessage(sender = sender, text = data, isSent = false)
                appendLog("Payload Received: [$sender] $data")
            }
        }

        broadcastService.onStatusChanged = { status ->
            runOnUiThread {
                tvStatus.text = "Connection Status: $status"
                val connected = broadcastService.isConnected()
                btnBroadcast.isEnabled = connected
                btnShutdown.isEnabled = true
                btnInitialize.isEnabled = !connected
            }
        }

        broadcastService.onLog = { log ->
            runOnUiThread {
                appendLog(log)
            }
        }
    }

    private fun setupClickListeners() {
        btnInitialize.setOnClickListener {
            broadcastService.initialize()
            btnInitialize.isEnabled = false
            btnShutdown.isEnabled = true
        }

        btnShutdown.setOnClickListener {
            broadcastService.shutdown()
            btnInitialize.isEnabled = true
            btnShutdown.isEnabled = false
            btnBroadcast.isEnabled = false
            tvStatus.text = "Connection Status: Disconnected"
        }

        btnBroadcast.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isEmpty()) {
                Snackbar.make(etMessage, "Type a message first", Snackbar.LENGTH_SHORT).show()
                appendLog("Empty message - broadcast cancelled")
                return@setOnClickListener
            }
            broadcastService.broadcast(message)
            addMessage(sender = broadcastService.deviceName, text = message, isSent = true)
            etMessage.text.clear()
        }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                btnBroadcast.performClick()
                true
            } else false
        }

        tabReceived.setOnClickListener { showMessagesTab() }
        tabLog.setOnClickListener { showLogTab() }
    }

    private fun showMessagesTab() {
        tabReceived.setTextColor(ContextCompat.getColor(this, R.color.red_primary))
        tabReceived.setTypeface(null, android.graphics.Typeface.BOLD)
        tabLog.setTextColor(ContextCompat.getColor(this, R.color.grey_medium))
        tabLog.setTypeface(null, android.graphics.Typeface.NORMAL)
        frameMessages.visibility = View.VISIBLE
        scrollLog.visibility = View.GONE
    }

    private fun showLogTab() {
        tabLog.setTextColor(ContextCompat.getColor(this, R.color.red_primary))
        tabLog.setTypeface(null, android.graphics.Typeface.BOLD)
        tabReceived.setTextColor(ContextCompat.getColor(this, R.color.grey_medium))
        tabReceived.setTypeface(null, android.graphics.Typeface.NORMAL)
        frameMessages.visibility = View.GONE
        scrollLog.visibility = View.VISIBLE
    }

    private fun addMessage(sender: String, text: String, isSent: Boolean) {
        val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val message = MessageModel(sender = sender, text = text, timestamp = timestamp, isSent = isSent)
        messageAdapter.addMessage(message)
        tvNoMessages.visibility = View.GONE
        rvMessages.scrollToPosition(0)
    }

    private fun appendLog(log: String) {
        val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $log\n"
        tvLog.append(logEntry)
        scrollLog.post { scrollLog.fullScroll(View.FOCUS_DOWN) }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // Location - required for Bluetooth discovery on Android 11 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val btPermissions = listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            for (perm in btPermissions) {
                if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(perm)
                }
            }
        }

        // Nearby Wi-Fi for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        // Legacy Bluetooth for Android 11 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE
            )
        } else {
            appendLog("All permissions already granted")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val denied = mutableListOf<String>()
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    denied.add(permissions[i].substringAfterLast("."))
                }
            }
            if (denied.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "Permission Denied: ${denied.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()
                appendLog("Permission Denied: ${denied.joinToString(", ")}")
            } else {
                appendLog("All Permissions Granted")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::broadcastService.isInitialized) {
            broadcastService.shutdown()
        }
    }
}
