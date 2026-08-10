package mx.utng.ich.safecaretv.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mx.utng.ich.safecaretv.data.alert.TvAlert
import mx.utng.ich.safecaretv.data.alert.TvAlertsRepository

class TvAlertsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TvAlertsRepository()
    private val preferences = application.getSharedPreferences("tv_acknowledged_alerts", 0)
    private val _activeAlert = MutableStateFlow<TvAlert?>(null)
    val activeAlert = _activeAlert.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(5_000)
            }
        }
    }

    fun acknowledge() {
        _activeAlert.value?.let { alert ->
            preferences.edit().putBoolean(alert.id, true).apply()
            _activeAlert.value = null
        }
    }

    private suspend fun refresh() {
        runCatching { repository.getActiveAlerts() }
            .onSuccess { alerts ->
                val nextAlert = alerts.firstOrNull {
                    !preferences.getBoolean(it.id, false)
                }
                val currentAlert = _activeAlert.value
                if (currentAlert == null ||
                    (nextAlert?.isSos == true && !currentAlert.isSos)
                ) {
                    _activeAlert.value = nextAlert
                }
            }
            .onFailure { Log.e("TvAlerts", "Error loading Supabase alerts", it) }
    }
}
