package xyz.zephr.sampleclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import xyz.zephr.sampleclient.ui.theme.ZephrSampleClientAppTheme
import xyz.zephr.sdk.api.ZephrTypes
import xyz.zephr.sdk.api.embedded.ZephrEmbeddedLocationManager

private const val TAG = "EmbeddedSolverActivity"

class EmbeddedSolverActivity : ComponentActivity() {
    private val zephrRequiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        // POST_NOTIFICATIONS only exists on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
    private var permissionsGranted by mutableStateOf(false)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            permissionsGranted = true
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

    private fun hasLocationPermissions() = zephrRequiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsGranted = hasLocationPermissions()

        setContent {
            ZephrSampleClientAppTheme {
                if (permissionsGranted) {
                    setupServiceObserver()
                    // A listener can be registered from anywhere and will get updates as long as the service is started
                    ZephrEmbeddedLocationManager.requestLocationUpdates(zephrListener)
                    SuccessScreen()
                } else {
                    RequestPermissionScreen(onRequest = { permissionLauncher.launch(zephrRequiredPermissions) })
                }
            }
        }
    }

    fun setupServiceObserver() {
        // This automatically triggers startService via ON_CREATE
        lifecycle.addObserver(EmbeddedLocationServiceObserver(this))
    }

    override fun onDestroy() {
        ZephrEmbeddedLocationManager.removeLocationUpdates(zephrListener)
        super.onDestroy()
    }

}

@Composable
private fun SuccessScreen() {
    var recentLocation by remember { mutableStateOf<ZephrTypes.ZephrLocation?>(null) }
    var showLocationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen when permissions are granted
        Text("Zephr Embedded SDK Running")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            recentLocation = ZephrEmbeddedLocationManager.getMostRecentLocation()
            showLocationDialog = true
        }) {
            Text("Get most recent location")
        }
    }

    if (showLocationDialog) {
        val location = recentLocation
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Most Recent Location") },
            text = {
                if (location != null) {
                    Text("Lat: ${location.latitude}\nLon: ${location.longitude}")
                } else {
                    Text("No location available yet.")
                }
            }
        )
    }
}

@Composable
private fun RequestPermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Permission Denied / Initial Screen
        val textToShow = "Location and Notification access is needed to start Zephr location service."

        Text(textToShow, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onRequest() }) {
            Text("Grant Permissions")
        }
    }
}
