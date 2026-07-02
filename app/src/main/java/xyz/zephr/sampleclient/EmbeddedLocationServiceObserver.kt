package xyz.zephr.sampleclient

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import xyz.zephr.sdk.api.embedded.ZephrEmbeddedLocationManager

/**
 * We can set up an observer on the mainActivity lifecycle to keep the SDK running for the lifecycle
 * of the entire app. We also recommend a
 */
class EmbeddedLocationServiceObserver(private val context: Context) : LifecycleEventObserver {
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
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LocationServiceObserver", "Required permissions not acquired! Can't start service")
            return
        }
        ZephrEmbeddedLocationManager.start(context)
    }

    private fun stopService() {
        ZephrEmbeddedLocationManager.stop()
    }
}
