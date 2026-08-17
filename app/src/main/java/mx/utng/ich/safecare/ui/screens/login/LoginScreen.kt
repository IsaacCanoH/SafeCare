package mx.utng.ich.safecare.ui.screens.login

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
/** Muestra el formulario que valida e inicia la sesión del cuidador. */
fun LoginScreen(
    authState: AuthState = AuthState.Idle,
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
        return pass.length >= 6
    }

    AppTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(100.dp)
                    )

                    Text(
                        text = "Familia Segura",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Sistema de monitoreo y seguridad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Accede a tu cuenta para continuar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it 
                            emailError = if (validateEmail(it)) null else "Correo inválido"
                        },
                        label = { Text("Correo") },
                        isError = emailError != null,
                        supportingText = { emailError?.let { Text(it) } },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = authState !is AuthState.Loading
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it 
                            passwordError = if (validatePassword(it)) null else "La contraseña no puede estar vacía"
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
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = authState !is AuthState.Loading
                    )

                    // Login Button
                    Button(
                        onClick = {
                            if (validateEmail(email) && validatePassword(password)) {
                                onLoginClick(email, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
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
                            Text(
                                text = "Iniciar sesión",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "¿No tienes una cuenta? ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = onRegisterClick,
                            enabled = authState !is AuthState.Loading
                        ) {
                            Text(
                                text = "Regístrate",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
