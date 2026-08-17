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

/**
 * Estado sellado que representa las posibles fases de autenticación del cuidador.
 */
sealed class AuthState {
    /** Sin acción de autenticación en curso. */
    object Idle : AuthState()
    /** Operación de autenticación en progreso. */
    object Loading : AuthState()
    /** Autenticación completada exitosamente. */
    object Success : AuthState()
    /**
     * Error durante la autenticación.
     *
     * @property message Mensaje descriptivo del error para mostrar al usuario.
     */
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel que gestiona la autenticación del cuidador en SafeCare.
 *
 * Mantiene los estados de inicio de sesión y registro, autentica con correo y contraseña
 * mediante Supabase Auth, crea el usuario de dominio en la base de datos y devuelve
 * mensajes de error comprensibles a la interfaz de usuario.
 *
 * @param supabaseRepository Repositorio para operaciones de persistencia del usuario.
 */
class AuthViewModel(
    private val supabaseRepository: SupabaseRepository = SupabaseRepository()
) : ViewModel() {
    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    /** Estado observable de la autenticación para la interfaz de usuario. */
    val authState: State<AuthState> = _authState

    /**
     * Inicia sesión con correo y contraseña, actualizando el estado de autenticación.
     *
     * Fuerza un cierre de sesión previo para limpiar cualquier sesión residual
     * antes de intentar la autenticación.
     *
     * @param email Correo electrónico del cuidador.
     * @param pass Contraseña del cuidador.
     */
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

    /**
     * Cierra la sesión local y remota del cuidador.
     *
     * Restablece el estado de autenticación a [AuthState.Idle] independientemente
     * de si el cierre remoto fue exitoso.
     */
    fun logout() {
        viewModelScope.launch {
            runCatching { SupabaseClient.client.auth.signOut() }
                .onFailure { error -> Log.e("AuthVM", "Logout Error", error) }
            _authState.value = AuthState.Idle
        }
    }

    /**
     * Crea la cuenta en Supabase Auth y guarda el perfil del nuevo cuidador.
     *
     * El flujo es: (1) registrar en Supabase Auth, (2) obtener el ID del usuario,
     * (3) generar el hash de la contraseña y (4) guardar el usuario de dominio.
     *
     * @param name Nombre completo del cuidador.
     * @param email Correo electrónico para la cuenta.
     * @param pass Contraseña elegida por el cuidador.
     */
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
