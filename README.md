# Example Zephr Realtime Client App

This Android application demonstrates how to integrate and use the Zephr realtime sdk, a GNSS-based location service for Android.

---

## Features

- Fetch real-time location and pose data using the Zephr realtime sdk.

---

## Prerequisites

Before you begin, make sure you have:

- Android Studio installed.
- Basic knowledge of Android development and Kotlin/Java.
- A working internet connection.

---

## Installation

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Sync the project with Gradle.
4. Build and run the app on a physical Android device (recommended over emulator).

---

## Requirements

- **Minimum SDK version:** 31 (Android 12)
- **Minimum Kotlin version:** 2.1.0

---

## Usage

1. Launch the app on your device.
2. The app initializes and begins calculating your location.
3. Filter the logcat output using the tag `ZephrSampleClientApp` to see SDK logs.
4. The app will print position updates in latitude/longitude and pose updates like:

```
GNSS Update - Status: OK, Lat: 37.4219983, Lng: -122.084, Alt: 5.2
Pose Update - yaw: 121.3, pitch: 1.2, roll: 3.4
```

---

## Permissions

To use ZephrRealtimeSDK, your app must request and receive the `ACCESS_FINE_LOCATION` and
`ACCESS_COURSE_LOCATION` permissions, and with API 33 and later, `POST_NOTIFICATIONS`. An example
implementation is provided in `MainActivity.kt` using google accompanist.

```kotlin
@Composable
fun LocationTrackingScreen(onStart: () -> Unit, onStop: () -> Unit) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            // POST_NOTIFICATIONS only exists on API 33+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (permissionState.allPermissionsGranted) {
            // Screen when permissions are granted
            Text("Location Permissions Granted")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onStart) { Text("Start Zephr Service") }
            Button(onClick = onStop) { Text("Stop Zephr Service") }
        } else {
            // Permission Denied / Initial Screen
            val textToShow = if (permissionState.shouldShowRationale) {
                "Location and Notification access is needed to start Zephr location service."
            } else {
                "This feature requires Location and Notification permissions."
            }

            Text(textToShow, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    }
}
```
However, if you do not wish to add a dependency, you can check for permissions natively.

```kotlin
@Composable
fun LocationTrackingScreen(onStart: () -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    
    val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        // POST_NOTIFICATIONS only exists on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
    
    var permissionsGranted by remember {
        mutableStateOf(requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
        // Check if all permissions in the map are true
        permissionsGranted = resultMap.values.all { it }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (permissionsGranted) {
            // Screen when permissions are granted
            Text("Location Permissions Granted")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onStart) { Text("Start Zephr Service") }
            Button(onClick = onStop) { Text("Stop Zephr Service") }
        } else {
            // Permission Denied / Initial Screen
            val textToShow = "Location and Notification access is needed to start Zephr location service."

            Text(textToShow, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { launcher.launch(requiredPermissions) }) {
                Text("Grant Permissions")
            }
        }
    }
}
```

Also ensure the following permissions are declared in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## SDK Integration

### 1. Create a zephr account and setup SDK authentication

- Sign up here: https://zephr.xyz/auth/signup
- Follow these instructions to authenticate your app with Zephr backend services that the SDK depends on: https://zephr.xyz/developer-portal/cert-digest-instructions 

---

### 2. Add the Zephr SDK to Your Gradle Build

Just add the SDK to your `dependencies` block:

```kotlin
// NOTE: during soft launch, new zephr sdk releases will be cut regularly
// please prefer to depend on latest point release to ensure
// you get the latest fixes and improvements
implementation("xyz.zephr.sdk.final:positioning:0.3.+")
```

---

### 3. Initialize the SDK

Initialize the SDK and attach handlers to receive the sdk output using the builder pattern, as shown in `MainActivity.kt` with relevant snippets reproduced below:

```kotlin
ZephrLocationManager.requestLocationUpdates(object : ZephrEventListener {
    override fun onZephrLocationChanged(zephrLocationEvent: ZephrLocationEvent) {
        val status = zephrLocationEvent.status
        val location = zephrLocationEvent.location
        if (location != null) {
            Log.d(
                TAG,
                "GNSS Update - Status: $status, Lat: ${location.latitude}, Lng: ${location.longitude}, Alt: ${location.altitude}"
            )
        } else {
            Log.d(TAG, "GNSS Update - Status: $status, Location: null")
        }
    }

    override fun onPoseChanged(
        zephrPoseEvent: ZephrPoseEvent
    ) {
        Log.d(
            TAG,
            "Pose Update - yaw: ${zephrPoseEvent.yprWithTimestamp?.first?.get(0)} pitch: ${zephrPoseEvent.yprWithTimestamp?.first?.get(1)} roll: ${zephrPoseEvent.yprWithTimestamp?.first?.get(2)}"
        )
    }
})

ZephrLocationManager.start(this) // Pass your context here, which may be "this" within an activity
```

To stop updates and shut down the location service, run:

```kotlin
ZephrLocationManager.stop(this) // Pass your context here, which may be "this" within an activity
```

---

## License

The sample code in this repository is licensed under the MIT License
