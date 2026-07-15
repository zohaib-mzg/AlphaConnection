# AlphaConnection

A Proof-of-Concept Android application demonstrating reliable local broadcast communication between nearby Android phones using **Google Nearby Connections API**. No internet required.

**Developed by MUHAMMAD ZOHAIB**

---

## How It Works

AlphaConnection uses Google Nearby Connections API to discover and connect nearby Android devices over Bluetooth and Wi-Fi Direct. Once connected, devices can exchange broadcast messages without any internet connection.

### Architecture

| Class | Purpose |
|-------|---------|
| `SplashActivity` | Simple splash screen with fade-in animation |
| `MainActivity` | Main UI with controls, message list, and log |
| `LocalBroadcastService` | Wraps the API: `initialize()`, `broadcast()`, `onReceive()`, `shutdown()` |
| `NearbyManager` | Low-level Google Nearby Connections wrapper |
| `MessageAdapter` | RecyclerView adapter for displaying messages |
| `MessageModel` | Data class for message text, timestamp, and direction |

### API

```kotlin
initialize()                    // Start advertising + discovery
broadcast(data: String)         // Send message to all connected devices
onReceive(data: String)         // Callback when message received
shutdown()                      // Stop all connections
```

### Communication Flow

1. **Initialize** - Device starts advertising (making itself visible) and scanning (looking for other devices)
2. **Auto-Connect** - Nearby devices automatically discover and connect to each other
3. **Broadcast** - Type a message and tap Broadcast to send it to all connected devices
4. **Receive** - Incoming messages appear in the Received Messages list in real-time
5. **Shutdown** - Stop all advertising, discovery, and connections

---

## How to Run

### Prerequisites
- Android Studio (Hedgehog or later recommended)
- Two Android phones with Bluetooth and Wi-Fi enabled
- Both phones must have the app installed
- Google Play Services installed on both phones

### Steps
1. Open the project in Android Studio
2. Build and install the APK on both phones
3. Grant all permissions when prompted (Location, Bluetooth)
4. On both phones, tap **Initialize**
5. Wait for devices to discover each other (status shows "Connected")
6. Type a message and tap **Broadcast**
7. The message appears on both phones

---

## How to Test with Two Android Phones

### Setup
1. Install the app on Phone A and Phone B
2. Enable **Bluetooth** and **Wi-Fi** on both phones
3. Keep phones within 10-30 feet of each other

### Test Procedure
1. **Phone A**: Open the app → Tap **Initialize**
   - Status shows: "Advertising..." or "Discovering..."
2. **Phone B**: Open the app → Tap **Initialize**
   - Status on both phones should change to: "Connected (1 device(s))"
3. **Phone A**: Type "Hello from Phone A" → Tap **Broadcast**
   - Both phones show the message in the received list
4. **Phone B**: Type "Hello from Phone B" → Tap **Broadcast**
   - Both phones show the message
5. Tap the **Log** tab to see connection events and message history
6. Tap **Shutdown** to disconnect

### Troubleshooting
- If devices don't connect, ensure both have location services enabled
- Try turning Bluetooth off and on again
- Keep phones close together (within 5 meters for best results)
- Ensure Google Play Services is up to date on both devices

---

## Technical Details

- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)
- **Strategy**: STAR (one-to-many broadcast topology)
- **Service ID**: `com.alphaconnection.poc`
- **Transport**: Bluetooth + Wi-Fi Direct (automatic)
- **Language**: 100% Kotlin
- **UI**: Material Design 3
- **Dependencies**: Google Nearby Connections only

## Permissions Required

| Permission | Purpose |
|-----------|---------|
| `BLUETOOTH` / `BLUETOOTH_SCAN` | Device discovery |
| `BLUETOOTH_ADVERTISE` | Make device visible |
| `BLUETOOTH_CONNECT` | Establish connections |
| `ACCESS_FINE_LOCATION` | Required for Bluetooth discovery |
| `ACCESS_WIFI_STATE` | Wi-Fi Direct transport |
| `CHANGE_WIFI_STATE` | Wi-Fi Direct transport |
