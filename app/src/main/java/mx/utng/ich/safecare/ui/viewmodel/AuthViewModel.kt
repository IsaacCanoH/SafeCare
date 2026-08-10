package mx.utng.ich.safecare.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import mx.utng.ich.safecare.data.local.entity.UsuarioEntity
import mx.utng.ich.safecare.data.remote.SupabaseClient
import mx.utng.ich.safecare.data.repository.SupabaseRepository
import mx.utng.ich.safecare.util.SecurityUtils
import android.util.Log

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val supabaseRepository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Forzamos un logout previo para limpiar cualquier sesión "fantasma"
                try { SupabaseClient.client.auth.signOut() } catch (e: Exception) {}

                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("AuthVM", "Login Error: ${e.message}")
                val errorMessage = when {
                    e.message?.contains("Invalid login credentials", ignoreCase = true) == true -> 
                        "Correo o contraseña incorrectos"
                    e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                        "Por favor confirma tu correo electrónico"
                    else -> "Error de conexión o datos inválidos"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // 1. Registro en Supabase Auth
                val authResponse = SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                
                // 2. Usar siempre el ID real devuelto por Auth, incluso si el correo
                // todavía requiere confirmación y no existe una sesión local.
                val userId = authResponse?.id
                    ?: SupabaseClient.client.auth.currentSessionOrNull()?.user?.id
                    ?: error("Supabase no devolvio el identificador del usuario")
                
                // 3. Hashear la contraseña para nuestras tablas de perfil
                val passwordHash = SecurityUtils.hashPassword(pass)
                
                val newUsuario = UsuarioEntity(
                    idUsuario = userId,
                    nombre = name,
                    correo = email,
                    contrasena = passwordHash,
                    estado = true
                )

                // 4. Guardar en Supabase DB (Tabla personalizada 'usuario')
                check(supabaseRepository.saveUser(newUsuario)) {
                    "No se pudo guardar el perfil de usuario en Supabase"
                }
                
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                Log.e("AuthVM", "Register Error: ${e.message}")
                val errorMessage = when {
                    e.message?.contains("User already registered", ignoreCase = true) == true ->
                        "Este correo ya está registrado"
                    e.message?.contains("Signup disabled", ignoreCase = true) == true ->
                        "El registro está deshabilitado temporalmente"
                    else -> "Error: Verifica tu conexión e intenta de nuevo"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }
}
