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

To use ZephrRealtimeSDK, your app must request and receive the `ACCESS_FINE_LOCATION` permission. An example implementation is provided in `MainActivity.kt`.

```kotlin
private fun checkLocationPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        startLocationUpdates()
    } else {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startLocationUpdates()
            } else {
                // Handle denial
            }
        }.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
```

Also ensure the following permissions are declared in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

## SDK Integration

### 1. Get Access Token for preview release

Contact the zephr team to get a token to access our private repo to access the preview release.

---

### 2. Set the Access Token

The token should be supplied via gradle property:

#### Example A: Gradle user properties file

Add this line to `~/.gradle/gradle.properties`:

```
zephr_maven_repo.password=BASE64_ACCESS_TOKEN
```

> ⚠️ Do **not** wrap the token in quotes. Ensure it is on one line.

#### Example B: Gradle local.properties

Note: local.properties is not typically committed to vcs
Add this line to the local.properties file in your android build root

```
zephr_maven_repo.password=BASE64_ACCESS_TOKEN
```

> ⚠️ Do **not** wrap the token in quotes. Ensure it is on one line.

---

### 3. Add Zephr SDK to Your Gradle Build

In your `build.gradle.kts`, add the Zephr private Maven repo inside the `repositories` block:

```kotlin
maven {
    url = uri("https://us-central1-maven.pkg.dev/zephr-xyz-firebase-development/maven-repo")
    credentials {
        username = "_json_key_base64"
        password = findProperty("zephr_maven_repo.password") as String?
            ?: throw GradleException("Missing required gradle property needed to access zephr maven repo: 'zephr_maven_repo.password'")
    }
    authentication {
        create<BasicAuthentication>("basic")
    }
}
```

Then, add the SDK to your `dependencies` block:

```kotlin
implementation("com.zephr.library.pr-0:zephrLib:0.0.1-SNAPSHOT") {
    isChanging = true // SDK updates frequently; always fetch latest during preview.
}
```

Tell Gradle to check for updated snapshots hourly:

```kotlin
configurations.all {
    resolutionStrategy.cacheChangingModulesFor(1, "hours")
}
```

Sync your project after making these changes.

---

### 4. Initialize the SDK

You can initialize the SDK and attach handlers for the sdk output using the builder pattern, as shown in `MainActivity.kt`:

```kotlin
  val zephrRealtimeSDK = ZephrRealtimeSDK.Builder(this.baseContext).build()

  zephrRealtimeSDK.requestLocationUpdates(object : ZephrEventListener {
      override fun onZephrGnssReceived(zephrGnssEvent: com.zephr.sdk.v2.model.ZephrGnssEvent) {
          val status = zephrGnssEvent.status
          val location = zephrGnssEvent.location
          if (location != null) {
              Log.d(TAG, "GNSS Update - Status: $status, Lat: ${location.latitude}, Lng: ${location.longitude}, Alt: ${location.altitude}")
          } else {
              Log.d(TAG, "GNSS Update - Status: $status, Location: null")
          }
      }

      override fun onPoseChanged(
          zephrPoseEvent: ZephrPoseEvent
      ) {
          Log.d(TAG, "Pose Update - yaw: ${zephrPoseEvent.yprWithTimestamp?.first[0]} pitch: ${zephrPoseEvent.yprWithTimestamp?.first[1]} roll: ${zephrPoseEvent.yprWithTimestamp?.first[2]}")
      }
  })

  serviceScope.launch {
      zephrRealtimeSDK.start()
  }
```

---

## License

The sample code in this repository is licensed under the MIT License
