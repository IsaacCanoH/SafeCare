package mx.utng.ich.safecare.data.local.entity

/**
 * Modelo de datos del perfil del cuidador registrado en la aplicación.
 *
 * Representa al usuario autenticado que supervisa a las personas monitoreadas.
 * Se almacena en la tabla de usuarios de Supabase después del registro.
 *
 * @property idUsuario Identificador único del usuario, generado por Supabase Auth.
 * @property nombre Nombre completo del cuidador.
 * @property correo Correo electrónico utilizado para la autenticación.
 * @property contrasena Hash SHA-256 de la contraseña del usuario.
 * @property telefono Número de teléfono de contacto, o `null` si no se proporcionó.
 * @property estado Estado activo de la cuenta; `true` si está habilitada.
 */
data class UsuarioEntity(
    val idUsuario: String,
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val telefono: String? = null,
    val estado: Boolean = true
)
