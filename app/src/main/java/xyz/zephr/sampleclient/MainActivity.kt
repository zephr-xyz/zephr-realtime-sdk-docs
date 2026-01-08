package xyz.zephr.sampleclient

import android.Manifest
import android.content.Context
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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import xyz.zephr.sampleclient.ui.theme.ZephrSampleClientAppTheme
import xyz.zephr.sdk.api.ZephrLocationManager
import xyz.zephr.sdk.api.ZephrTypes

private const val TAG = "ZephrSampleClientApp"

class MainActivity : ComponentActivity() {
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
                    ZephrLocationManager.requestLocationUpdates(zephrListener)
                    SuccessScreen()
                } else {
                    RequestPermissionScreen(onRequest = { permissionLauncher.launch(zephrRequiredPermissions) })
                }
            }
        }
    }

    fun setupServiceObserver() {
        // This automatically triggers startService via ON_CREATE
        lifecycle.addObserver(LocationServiceObserver(this))
    }

    override fun onDestroy() {
        ZephrLocationManager.removeLocationUpdates(zephrListener)
        super.onDestroy()
    }

}

/**
 * We can set up an observer on the mainActivity lifecycle to keep the SDK running for the lifecycle
 * of the entire app. We also recommend a
 */
class LocationServiceObserver(private val context: Context) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                // Ensure permissions are actually granted before this is called
                startService()
            }
            Lifecycle.Event.ON_DESTROY -> {
                stopService()
            }
            else -> {} // Ignore other events like ON_STOP if you want it to run in background
        }
    }

    private fun startService() {
        // Check permissions first
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Required permissions not acquired! Can't start service")
            return
        }
        ZephrLocationManager.start(context)
    }

    private fun stopService() {
        ZephrLocationManager.stop(context)
    }
}

@Composable
fun SuccessScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen when permissions are granted
        Text("Zephr SDK Running")
    }
}

@Composable
fun RequestPermissionScreen(onRequest: () -> Unit) {
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