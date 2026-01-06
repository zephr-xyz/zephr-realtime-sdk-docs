package xyz.zephr.sampleclient

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import xyz.zephr.sampleclient.ui.theme.ZephrSampleClientAppTheme
import xyz.zephr.sdk.api.ZephrLocationManager
import xyz.zephr.sdk.api.ZephrTypes

private const val TAG = "ZephrSampleClientApp"

class MainActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZephrSampleClientAppTheme {
                LocationTrackingScreen(
                    onStart = { startLocationUpdates() },
                    onStop = { stopLocationUpdates() }
                )
            }
        }
    }

    val zephrListener = object : ZephrTypes.ZephrEventListener {
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
                "Pose Update - yaw: ${zephrPoseEvent.yprWithTimestamp?.first?.get(0)} pitch: ${
                    zephrPoseEvent.yprWithTimestamp?.first?.get(
                        1
                    )
                } roll: ${zephrPoseEvent.yprWithTimestamp?.first?.get(2)}"
            )
        }
    }

    override fun onDestroy() {
        ZephrLocationManager.stop(this)
        super.onDestroy()
    }

    private fun startLocationUpdates() {
        try {
            ZephrLocationManager.start(this)
            ZephrLocationManager.requestLocationUpdates(zephrListener)
        } catch ( e: SecurityException) {
            Log.e(TAG, "Permissions not granted, error: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        ZephrLocationManager.removeLocationUpdates(zephrListener)
        ZephrLocationManager.stop(this)
        Log.d(TAG, "Zephr updates stopped.")
    }
}

@OptIn(ExperimentalPermissionsApi::class)
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