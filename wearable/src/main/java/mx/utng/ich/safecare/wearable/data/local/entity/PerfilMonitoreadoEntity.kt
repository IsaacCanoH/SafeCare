package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PerfilMonitoreado")
data class PerfilMonitoreadoEntity(
    @PrimaryKey
    val idPerfil: String,
    val nombre: String,
    val edad: Int,
    val fechaNacimiento: String? = null,
    val tipoPerfil: String,
    val foto: String? = null,
    val estadoActual: Boolean,
    val idCuidador: String
)
