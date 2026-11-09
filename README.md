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

- **Minimum SDK version:** 26 (Android 8.0)
- **Minimum Kotlin version:** 2.1.0
- **Minimum JDK toolchain:** Java 11

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
`ACCESS_COURSE_LOCATION` permissions, and with API 33 and later, `POST_NOTIFICATIONS`. We have
provided a dependency-free example of a permissions flow in this sample app, using
`ActivityResultContracts.RequestMultiplePermissions()`, like below:

```kotlin
private val zephrRequiredPermissions = mutableListOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
).apply {
    // POST_NOTIFICATIONS only exists on API 33+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { result ->
    val allGranted = result.values.all { it }
    if (allGranted) {
        permissionsGranted = true
    }
}
```

Which can be launched with `permissionLauncher.launch(zephrRequiredPermissions)`. 

Another common permission flow can is to use google accompanist permissions within Compose,
like below:

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
implementation("xyz.zephr.sdk.final:positioning:0.5.2")
```

Apps upgrading from `0.4.x` should follow the migration instructions below. A
`0.4.+` dependency stays on the 0.4 release line and will not select 0.5.2.

---

### 3. Initialize the SDK

After granting runtime permissions, start the SDK and register a listener, as
demonstrated in `BackgroundActivity.kt` and `LocationServiceObserver.kt`:

```kotlin
import xyz.zephr.sdk.api.ZephrLocationManager
import xyz.zephr.sdk.api.ZephrTypes

ZephrLocationManager.start(this) // Pass your context here, which may be "this" within an activity

ZephrLocationManager.requestLocationUpdates(object : ZephrTypes.ZephrEventListener {
    override fun onZephrLocationChanged(zephrLocationEvent: ZephrTypes.ZephrLocationEvent) {
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
        zephrPoseEvent: ZephrTypes.ZephrPoseEvent
    ) {
        Log.d(
            TAG,
            "Pose Update - yaw: ${zephrPoseEvent.yprWithTimestamp?.first?.get(0)} pitch: ${zephrPoseEvent.yprWithTimestamp?.first?.get(1)} roll: ${zephrPoseEvent.yprWithTimestamp?.first?.get(2)}"
        )
    }
})
```

Register the listener again after every start or restart. `start()` returns
immediately; use `sdkStateFlow` to observe startup and `lastStartError` if startup
fails. See the 0.4-to-0.5 migration section for lifecycle details.

To stop updates and shut down the location service, run:

```kotlin
ZephrLocationManager.stop(this) // Pass your context here, which may be "this" within an activity
```

## Synchronous location updates
To request the most recent location synchronously, call `ZephrLocationManager.getMostRecentLocation()`
while the SDK is running:

```kotlin
val recentLocation = ZephrLocationManager.getMostRecentLocation()
```

## Anti-Spoof mode

When running the Zephr positioning SDK in Anti-Spoof mode, the phone's
Android-provided position is suppressed as an input to the Zephr positioning
solution. The SDK continues to calculate position from raw GNSS measurements.
Android network-location fallback is also disabled, even if
`networkLocationFallbackEnabled` is set to `true`.

In our testing to date, Anti-Spoof mode has often outperformed regular mode on
lower-end Android devices.

Anti-Spoof mode is off by default. To enable it, pass `antiSpoofEnabled = true`
when starting the SDK after granting the required runtime permissions:

```kotlin
import xyz.zephr.sdk.api.ZephrLocationManager

ZephrLocationManager.start(
    context = applicationContext,
    antiSpoofEnabled = true,
)
```

Register your location listener after this call as shown in the initialization
example above. Permissions, foreground-service behavior, and listener APIs are
the same as in regular mode. Because Android position fallback is suppressed,
location updates can contain a `null` location until Zephr has a solution.

The same `antiSpoofEnabled = true` option is available on
`ZephrEmbeddedLocationManager.start()` for embedded integrations.

## Usage without an SDK-managed foreground service

For an app that already runs its own location foreground service, 0.5 adds an
option to keep using the asynchronous `ZephrLocationManager` API without starting
another service or notification:

```kotlin
import xyz.zephr.sdk.api.ForegroundServiceMode
import xyz.zephr.sdk.api.ZephrLocationManager

// Call after the host app's location foreground service is running.
ZephrLocationManager.start(
    context = applicationContext,
    foregroundServiceMode = ForegroundServiceMode.NONE,
)
```

`NONE` does not reduce receiver or solver work. The host is responsible for its
own service, notification, and permissions. Without a host location foreground
service, Android can stop location and GNSS updates when the app leaves the
foreground.

`ZephrEmbeddedLocationManager` remains available for hosts that need an API
without coroutine types, including Java apps and ATAK plugins. It also runs
without an SDK-managed service or notification and does not itself require
`POST_NOTIFICATIONS`. Foreground-only apps must stop tracking when they leave the
foreground; continued background tracking requires a host location foreground
service. Import it from `xyz.zephr.sdk.api.embedded.ZephrEmbeddedLocationManager`.
Its stop call takes **no context** and blocks until shutdown completes:

```kotlin
ZephrEmbeddedLocationManager.stop()
```

Call this blocking stop on an application-owned worker thread, never directly
from an SDK event callback. A callback-thread stop throws `IllegalStateException`.

## Migrating from 0.4.x to 0.5.x

The main change in 0.5.0 is a shared SDK core and explicit foreground-service
selection. The Maven coordinates, `xyz.zephr.sdk.api` imports, listener types, and
minimum SDK requirements are unchanged. A normal
`ZephrLocationManager.start(context)` still defaults to
`ForegroundServiceMode.MANAGED`, which starts the SDK's location foreground
service and notification. Existing integrations using only that manager can
keep this call.

### 1. Update the dependency

Replace `xyz.zephr.sdk.final:positioning:0.4.+` (or a pinned 0.4 version) with
`xyz.zephr.sdk.final:positioning:0.5.2`, then sync and rebuild the app.

### 2. Run only one manager at a time

`ZephrLocationManager` and `ZephrEmbeddedLocationManager` now share one in-process
SDK. Starting one while the other is starting, running, or stopping throws
`IllegalStateException`.

When switching from `ZephrLocationManager`, call `stop(context)` and wait for
`sdkStateFlow` to report `ZephrSdkState.STOPPED` before starting the embedded
manager. `stop(context)` returns before shutdown finishes. When switching in the
other direction, wait for `ZephrEmbeddedLocationManager.stop()` to return before
starting `ZephrLocationManager`. Keep mode switches serialized in your app.

### 3. Observe lifecycle state and startup failures

Use `sdkState` (or `ZephrLocationManager.sdkStateFlow`) to distinguish `STARTING`,
`RUNNING`, `STOPPING`, and `STOPPED`. The deprecated `isServiceRunning` and
`isRunning` booleans cannot tell you whether shutdown has completed. Check for
`STOPPED` before handing the SDK to the other manager.

`ZephrLocationManager.start()` and `stop()` are asynchronous, including in `NONE`
mode. If startup transitions from `STARTING` to `STOPPED` without reaching
`RUNNING`, inspect `lastStartError`. Failures discovered asynchronously, such as
missing permissions or service startup failures, cannot be handled solely by a
`try/catch` around `start()`.

### 4. Register listeners for each run

Listeners attached to a running SDK are dropped when it stops or restarts.
Call `requestLocationUpdates(listener)` again after each `start()` call.
Listeners registered while the SDK is stopped are queued for the next start.

Calling `ZephrLocationManager.start()` again restarts the SDK with the new
configuration. Calling `ZephrEmbeddedLocationManager.start()` while it is already
starting or running is ignored, including any new options; stop it first to
change configuration. Embedded shutdown blocks and must run outside SDK
callbacks, as described above.

### 5. Choose who manages the foreground service

Keep the default `MANAGED` mode when the SDK should provide its own service.
Use `ForegroundServiceMode.NONE` only when your app already provides a location
foreground service. You do not have to switch embedded integrations to
`ZephrLocationManager`; the embedded API remains supported.

The 0.5.1 and 0.5.2 releases do not require another change to these public
integration APIs. Upgrade directly to 0.5.2 using the steps above.

## Updating from 0.3.x to 0.4.x

Version 0.4.0 raises the minimum Java bytecode compatibility from Java 8 to Java 11, so if your app is compiled with a toolchain older than Java 11 it may not compile with our SDK dependency.

## Migrating from 0.2.x to 0.3.x

As of version 0.3.0, the import paths and data type namespace has changed from version 0.2.x. The
import path for all symbols has changed from `xyz.zephr.sdk.v2` to `xyz.zephr.sdk.api`, and all data
types can be imported under the namespace `xyz.zephr.sdk.api.ZephrTypes`, and will be preceded by
`ZephrTypes.` when invoked.


So, the following in 0.2.x:
```kotlin
import xyz.zephr.sdk.v2.ZephrEventListener
import xyz.zephr.sdk.v2.ZephrLocationManager
import xyz.zephr.sdk.v2.model.ZephrLocationEvent
import xyz.zephr.sdk.v2.model.ZephrPoseEvent

ZephrLocationManager.requestLocationUpdates(object : ZephrEventListener {
    override fun onZephrLocationChanged(zephrLocationEvent: ZephrLocationEvent) {

    }

    override fun onPoseChanged(
        zephrPoseEvent: ZephrPoseEvent
    ) {

    }
})
```

Would become the following as of 0.3.0:
```kotlin
import xyz.zephr.sdk.api.ZephrLocationManager
import xyz.zephr.sdk.api.ZephrTypes

ZephrLocationManager.requestLocationUpdates(object : ZephrTypes.ZephrEventListener {
    override fun onZephrLocationChanged(zephrLocationEvent: ZephrTypes.ZephrLocationEvent) {
        
    }

    override fun onPoseChanged(
        zephrPoseEvent: ZephrTypes.ZephrPoseEvent
    ) {
        
    }
})
```

---

## License

The sample code in this repository is licensed under the MIT License
