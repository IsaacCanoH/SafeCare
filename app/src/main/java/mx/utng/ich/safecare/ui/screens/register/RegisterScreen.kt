package mx.utng.ich.safecare.ui.screens.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import mx.utng.ich.safecare.designsystem.theme.AppTheme
import mx.utng.ich.safecare.ui.viewmodel.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/** Muestra el formulario que valida y registra un nuevo cuidador. */
fun RegisterScreen(
    authState: AuthState = AuthState.Idle,
    onBackClick: () -> Unit = {},
    onRegisterSuccess: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            snackbarHostState.showSnackbar(authState.message)
        }
    }

    /** Comprueba que el correo tenga un formato válido. */
    fun validateEmail(mail: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()
    }

    /** Comprueba que la contraseña tenga la longitud mínima. */
    fun validatePassword(pass: String): Boolean {
        // Mínimo 6 caracteres (estándar de Supabase por defecto)
        return pass.length >= 6
    }

    AppTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Crear cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    enabled = authState !is AuthState.Loading
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = if (validateEmail(it)) null else "Correo inválido"
                    },
                    label = { Text("Correo electrónico") },
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = authState !is AuthState.Loading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = if (validatePassword(it)) null else "Mínimo 6 caracteres"
                    },
                    label = { Text("Contraseña") },
                    isError = passwordError != null,
                    supportingText = { passwordError?.let { Text(it) } },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = authState !is AuthState.Loading
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") },
                    isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                    supportingText = { if (confirmPassword.isNotEmpty() && confirmPassword != password) Text("Las contraseñas no coinciden") },
                    leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = authState !is AuthState.Loading
                )

                Button(
                    onClick = {
                        if (validateEmail(email) && validatePassword(password) && password == confirmPassword) {
                            onRegisterSuccess(name, email, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Registrarse", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
