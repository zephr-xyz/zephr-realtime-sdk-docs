package xyz.zephr.sampleclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import xyz.zephr.sampleclient.ui.theme.ZephrSampleClientAppTheme
import xyz.zephr.sdk.api.ZephrLocationManager
import xyz.zephr.sdk.api.ZephrTypes

class MainActivity : ComponentActivity() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZephrSampleClientAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppDetails()
                }
            }
        }
        checkLocationPermission()
    }

    override fun onDestroy() {
        ZephrLocationManager.stop(this)
        super.onDestroy()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS])
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        ZephrLocationManager.start(this)
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
    }
}

private const val TAG = "ZephrSampleClientApp"

@Composable
fun AppDetails(modifier: Modifier = Modifier) {
    Text(
        text = "Zephr Location SDK sample app",
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    )
}

@Preview(showBackground = true)
@Composable
fun AppDetailsPreview() {
    ZephrSampleClientAppTheme {
        AppDetails()
    }
}