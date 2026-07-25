package mx.utng.ich.safecare.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mx.utng.ich.safecare.data.local.dao.UbicacionDao
import mx.utng.ich.safecare.data.local.entity.LatestProfileLocation

class LocationViewModel(
    ubicacionDao: UbicacionDao
) : ViewModel() {
    val latestLocationsByProfile: StateFlow<Map<String, LatestProfileLocation>> =
        ubicacionDao.observarUltimasPorPerfil()
            .map { locations -> locations.associateBy { it.idPerfil } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )
}
