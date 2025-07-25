package xyz.zephr.sampleclient

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import xyz.zephr.sampleclient.ui.theme.ZephrSampleClientAppTheme
import com.zephr.sdk.v2.ZephrEventListener
import com.zephr.sdk.v2.ZephrRealtimeManager
import com.zephr.sdk.v2.model.ZephrPoseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.zephr.sampleclient.service.ZephrGnssService

class MainActivity : ComponentActivity() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZephrSampleClientAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.Center))
                    {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally // Center children horizontally
                        ) {
                            AppDetails()
                            Spacer(modifier = Modifier.height(16.dp)) // Optional spacing
                            LaunchForegroundServiceButton(this@MainActivity)
                        }
                    }
                }

            }
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission given, initialize ZephrRealtimeSDK
            startLocationUpdates()
        } else {
            // Permission NOT given (yet)
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
                if (permission) {
                    // User granted, initialize ZephrRealtimeSDK
                    startLocationUpdates()
                } else {
                    // User Declined
                }
            }.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startLocationUpdates() {
        val zephrRealtimeSDK = ZephrRealtimeManager.getZephrSDK(this)

        zephrRealtimeSDK.requestLocationUpdates(object : ZephrEventListener {
            override fun onZephrGnssReceived(zephrGnssEvent: com.zephr.sdk.v2.model.ZephrGnssEvent) {
                val status = zephrGnssEvent.status
                val location = zephrGnssEvent.location
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

        serviceScope.launch {
            zephrRealtimeSDK.start()
        }
    }
}



private const val TAG = "ZephrSampleClientApp"

@Composable
fun AppDetails() {
    Text(
        text = "Zephr Location SDK sample app"
    )
}

@Preview(showBackground = true)
@Composable
fun AppDetailsPreview() {
    ZephrSampleClientAppTheme {
        AppDetails()
    }
}

@Composable
fun LaunchForegroundServiceButton(context: Context) {
    Button(onClick = {
        // Stop other logging
        val zephrRealtimeSDK = ZephrRealtimeManager.getZephrSDK(context)
        zephrRealtimeSDK.removeAllLocationUpdates()
        // Launch service
        val intent = Intent(context, ZephrGnssService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }) {
        Text("Launch Foreground Service")
    }
}