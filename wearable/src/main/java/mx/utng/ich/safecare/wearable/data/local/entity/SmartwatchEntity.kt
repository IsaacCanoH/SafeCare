package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smartwatch")
data class SmartwatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val numeroSerie: String,
    val bateria: Int,
    val conexion: String,
    val ultimaConexion: Long = System.currentTimeMillis(),
    val motivo: String = "REGISTRO",
    val idPerfil: String? = null,
    val sincronizado: Boolean = false
)
