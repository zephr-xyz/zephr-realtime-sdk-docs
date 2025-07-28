package xyz.zephr.sampleclient

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.zephr.sampleclient.data.repo.ZephrGnssRepo
import xyz.zephr.sampleclient.service.ZephrGnssService
import xyz.zephr.sampleclient.ui.location.LocationViewModel
import xyz.zephr.sampleclient.ui.theme.ZephrSampleClientAppTheme

private const val TAG = "ZephrSampleClientApp"

class MainActivity : ComponentActivity() {
    private var service: ZephrGnssService? = null
    private lateinit var viewModel: LocationViewModel

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as ZephrGnssService.GnssServiceBinder

            viewModel = ViewModelProvider(
                this@MainActivity,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return LocationViewModel(ZephrGnssRepo(localBinder)) as T
                    }
                }
            )[LocationViewModel::class.java]

            viewModel.startTracking()

            lifecycleScope.launch {
                viewModel.latestMeasurement.collect { zephrGnssEvent ->
                    val status = zephrGnssEvent?.status
                    val location = zephrGnssEvent?.location
                    if (location != null) {
                        Log.d(
                            TAG,
                            "GNSS Update - Status: $status, Lat: ${location.latitude}, Lng: ${location.longitude}, Alt: ${location.altitude}"
                        )
                    } else {
                        Log.d(TAG, "GNSS Update - Status: $status, Location: null")
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.stopTracking()
            service = null
        }
    }

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
                            // TODO: put latest location here
                        }
                    }
                }
            }
        }
        checkLocationPermission()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
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
        val intent = Intent(this, ZephrGnssService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        startForegroundService(intent)
    }
}


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