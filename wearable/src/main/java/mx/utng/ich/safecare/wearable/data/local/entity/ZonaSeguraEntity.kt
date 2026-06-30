package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ZonaSegura")
data class ZonaSeguraEntity(
    @PrimaryKey
    val idZona: String,
    val nombre: String,
    val latitudCentro: Double,
    val longitudCentro: Double,
    val radioMetros: Double,
    val activa: Boolean,
    val idPerfil: String
)
