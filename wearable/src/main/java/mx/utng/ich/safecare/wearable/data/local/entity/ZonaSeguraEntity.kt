package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zona_segura")
data class ZonaSeguraEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_zona")
    val id: String,
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "latitud_centro")
    val latitudCentro: Double,
    @ColumnInfo(name = "longitud_centro")
    val longitudCentro: Double,
    @ColumnInfo(name = "radio_metros")
    val radioMetros: Double,
    @ColumnInfo(name = "activa")
    val activa: Boolean,
    @ColumnInfo(name = "id_perfil")
    val idPerfil: String
)
