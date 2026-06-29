package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alertas")
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tipoAlerta: String,
    val descripcion: String,
    val idPerfil: String,
    val idUbicacion: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val sincronizada: Boolean = false
)