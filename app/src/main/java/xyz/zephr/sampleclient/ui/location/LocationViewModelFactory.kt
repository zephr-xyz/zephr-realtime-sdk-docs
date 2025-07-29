package xyz.zephr.sampleclient.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import xyz.zephr.sampleclient.data.repo.ZephrGnssRepo

class LocationViewModelFactory(
    private val repository: ZephrGnssRepo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}