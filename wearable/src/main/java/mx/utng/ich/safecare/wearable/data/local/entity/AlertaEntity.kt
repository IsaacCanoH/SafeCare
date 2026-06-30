package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "Alertas")
data class AlertaEntity(
    @PrimaryKey
    val idAlerta: String = UUID.randomUUID().toString(),
    val tipoAlerta: String,
    val descripcion: String,
    val fechaHora: Long = System.currentTimeMillis(),
    val estado: String = "ACTIVA",
    val idPerfil: String,
    val idUbicacion: String? = null
)
