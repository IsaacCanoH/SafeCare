package mx.utng.ich.safecare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Usuario")
data class UsuarioEntity(
    @PrimaryKey
    val idUsuario: String,
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val telefono: String? = null,
    val estado: Boolean = true
)
