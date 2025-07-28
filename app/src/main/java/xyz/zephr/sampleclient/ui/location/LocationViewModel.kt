package xyz.zephr.sampleclient.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import xyz.zephr.sampleclient.data.repo.ZephrGnssRepo
import xyz.zephr.sdk.v2.model.ZephrGnssEvent

class LocationViewModel(
    private val repository: ZephrGnssRepo
) : ViewModel() {

    val latestMeasurement: StateFlow<ZephrGnssEvent?> =
        repository.gnssMeasurements.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun startTracking() = repository.startUpdates()
    fun stopTracking() = repository.stopUpdates()
}