package mx.utng.ich.safecare.data.local.entity

data class UsuarioEntity(
    val idUsuario: String,
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val telefono: String? = null,
    val estado: Boolean = true
)
