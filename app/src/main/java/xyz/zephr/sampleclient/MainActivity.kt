package xyz.zephr.sampleclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import xyz.zephr.sampleclient.data.repo.ZephrGnssRepo
import xyz.zephr.sampleclient.ui.location.LocationViewModel
import xyz.zephr.sampleclient.ui.location.LocationViewModelFactory
import xyz.zephr.sampleclient.ui.theme.ZephrSampleClientAppTheme
import xyz.zephr.sdk.v2.model.ZephrGnssEvent

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = ZephrGnssRepo(applicationContext)

        val factory = LocationViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LocationViewModel::class.java]

        checkLocationPermission()

        setContent {
            val zephrGnssEvent = viewModel.latestMeasurement.collectAsState()
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
                            LLADisplay(zephrGnssEvent.value)
                        }
                    }
                }
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission given, initialize ZephrRealtimeSDK
            viewModel.start()
        } else {
            // Permission NOT given (yet)
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
                if (permission) {
                    // User granted, initialize ZephrRealtimeSDK
                    viewModel.start()
                } else {
                    // User Declined
                }
            }.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}


@Composable
fun AppDetails() {
    Text(
        text = "Zephr Location SDK sample app"
    )
}

@Composable
fun LLADisplay(zephrGnssEvent: ZephrGnssEvent?) {
    val location = zephrGnssEvent?.location
    val status = zephrGnssEvent?.status
    Text(
        text = "Status: $status"
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Lat: ${location?.latitude}\n" +
                "Lon: ${location?.longitude}\n" +
                "Alt: ${location?.altitude}"
    )
}

@Preview(showBackground = true)
@Composable
fun AppDetailsPreview() {
    ZephrSampleClientAppTheme {
        AppDetails()
    }
}