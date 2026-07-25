package mx.utng.ich.safecaretv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mx.utng.ich.safecaretv.data.remote.TvSupabaseClient

sealed interface TvAuthState {
    data object CheckingSession : TvAuthState
    data object SignedOut : TvAuthState
    data object Loading : TvAuthState
    data class SignedIn(val email: String) : TvAuthState
    data class Error(val message: String) : TvAuthState
}

class TvAuthViewModel : ViewModel() {
    private val _state = MutableStateFlow<TvAuthState>(TvAuthState.CheckingSession)
    val state: StateFlow<TvAuthState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = TvSupabaseClient.client.auth.currentSessionOrNull()
            _state.value = session?.user?.email
                ?.let(TvAuthState::SignedIn)
                ?: TvAuthState.SignedOut
        }
    }

    fun login(email: String, password: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            _state.value = TvAuthState.Error("Ingresa tu correo y contraseña")
            return
        }

        viewModelScope.launch {
            _state.value = TvAuthState.Loading
            runCatching {
                TvSupabaseClient.client.auth.signInWith(Email) {
                    this.email = cleanEmail
                    this.password = password
                }
                val authenticatedEmail =
                    TvSupabaseClient.client.auth.currentSessionOrNull()?.user?.email
                        ?: cleanEmail
                TvAuthState.SignedIn(authenticatedEmail)
            }.onSuccess { authenticated ->
                _state.value = authenticated
            }.onFailure { error ->
                _state.value = TvAuthState.Error(error.toFriendlyMessage())
            }
        }
    }

    fun dismissError() {
        if (_state.value is TvAuthState.Error) {
            _state.value = TvAuthState.SignedOut
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { TvSupabaseClient.client.auth.signOut() }
            _state.value = TvAuthState.SignedOut
        }
    }

    private fun Throwable.toFriendlyMessage(): String = when {
        message?.contains("Invalid login credentials", ignoreCase = true) == true ->
            "Correo o contraseña incorrectos"
        message?.contains("Email not confirmed", ignoreCase = true) == true ->
            "Confirma tu correo antes de iniciar sesión"
        else -> "No se pudo iniciar sesión. Revisa tu conexión e inténtalo nuevamente"
    }
}
