package mx.utng.ich.safecare.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.entity.LatestProfileLocation
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.data.repository.SupabaseRepository

class LocationViewModel(
    private val repository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val _latestLocationsByProfile = MutableStateFlow<Map<String, LatestProfileLocation>>(emptyMap())
    val latestLocationsByProfile: StateFlow<Map<String, LatestProfileLocation>> = _latestLocationsByProfile
    private var realtimeJob: Job? = null

    fun refreshLocations(): Job? {
        val caregiverId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return null
        return viewModelScope.launch {
            runCatching { repository.fetchLatestLocationsForCaregiver(caregiverId) }
                .onSuccess { _latestLocationsByProfile.value = it.associateBy(LatestProfileLocation::idPerfil) }
        }
    }

    fun startRealtimeUpdates() {
        if (realtimeJob != null) return
        val caregiverId = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return
        val channel = SupabaseClient.client.channel("mobile-locations-$caregiverId")
        realtimeJob = viewModelScope.launch {
            runCatching {
                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "Ubicacion"
                }.collectLatest {
                    refreshLocations()?.join()
                }
            }.onFailure { exception ->
                Log.e(TAG, "Realtime locations", exception)
            }
        }
        viewModelScope.launch {
            runCatching { channel.subscribe(blockUntilSubscribed = true) }
                .onFailure { exception -> Log.e(TAG, "Realtime locations subscribe", exception) }
        }
    }

    private companion object {
        const val TAG = "LocationViewModel"
    }
}
