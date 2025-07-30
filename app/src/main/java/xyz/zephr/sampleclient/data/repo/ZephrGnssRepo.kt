package xyz.zephr.sampleclient.data.repo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat.startForegroundService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xyz.zephr.sampleclient.service.ZephrGnssService
import xyz.zephr.sdk.v2.model.ZephrGnssEvent

class ZephrGnssRepo(
    private val context: Context
) {
    private val _gnssFlow = MutableSharedFlow<ZephrGnssEvent>(replay = 1)
    val gnssMeasurements: Flow<ZephrGnssEvent> = _gnssFlow.asSharedFlow()

    private var service: ZephrGnssService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as ZephrGnssService.GnssServiceBinder
            service = localBinder.getService()

            localBinder.setGnssListener {
                _gnssFlow.tryEmit(it)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun bindService() {
        val intent = Intent(context, ZephrGnssService::class.java).also {
            context.bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        startForegroundService(context, intent)
    }

    fun unbindService() {
        context.unbindService(serviceConnection)
    }
}