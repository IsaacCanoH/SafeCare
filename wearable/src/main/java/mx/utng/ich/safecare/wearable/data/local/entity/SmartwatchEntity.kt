package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "SmartWatch")
data class SmartwatchEntity(
    @PrimaryKey
    val idSmartwatch: String = UUID.randomUUID().toString(),
    val numeroSerie: String,
    val bateria: Int,
    val conexion: String,
    val ultimaConexion: Long = System.currentTimeMillis(),
    val estado: String = "ACTIVO",
    val idPerfil: String? = null
)
