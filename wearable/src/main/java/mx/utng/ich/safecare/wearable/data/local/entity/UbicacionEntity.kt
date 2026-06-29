package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ubicaciones")
data class UbicacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: Long = System.currentTimeMillis(),
    val idSmartwatch: String,
    val sincronizada: Boolean = false
)
