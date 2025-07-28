package xyz.zephr.sampleclient.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xyz.zephr.sampleclient.service.ZephrGnssService
import xyz.zephr.sdk.v2.model.ZephrGnssEvent

class ZephrGnssRepo(
    private val serviceBinder: ZephrGnssService.GnssServiceBinder // your service connection
) {

    private val _gnssFlow = MutableSharedFlow<ZephrGnssEvent>(replay = 1)
    val gnssMeasurements: Flow<ZephrGnssEvent> = _gnssFlow.asSharedFlow()

    fun startUpdates() {
        serviceBinder.setGnssListener { data ->
            _gnssFlow.tryEmit(data)
        }
    }

    fun stopUpdates() {
        serviceBinder.removeGnssListener()
    }
}