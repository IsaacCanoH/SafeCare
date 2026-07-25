package mx.utng.ich.safecare.data.local.entity

import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "PerfilMonitoreado")
data class PerfilMonitoreadoEntity(
    @PrimaryKey
    val idPerfil: String = UUID.randomUUID().toString(),
    val nombre: String,
    val edad: Int,
    val fechaNacimiento: String? = null,
    val tipoPerfil: String,
    val foto: String? = null,
    val estadoActual: Boolean = true,
    val idCuidador: String
)

@Entity(tableName = "ZonaSegura")
data class ZonaSeguraEntity(
    @PrimaryKey
    val idZona: String = UUID.randomUUID().toString(),
    val nombre: String,
    val latitudCentro: Double,
    val longitudCentro: Double,
    val radioMetros: Double,
    val activa: Boolean = true,
    val idPerfil: String
)

@Entity(tableName = "SmartWatch")
data class SmartwatchEntity(
    @PrimaryKey
    val idSmartwatch: String = UUID.randomUUID().toString(),
    val numeroSerie: String,
    val watchInstallationId: String? = null,
    val nombreDispositivo: String? = null,
    val modelo: String? = null,
    val dataLayerNodeId: String? = null,
    val bateria: Int = 100,
    val conexion: String = "online",
    val ultimaConexion: Long = System.currentTimeMillis(),
    val idPerfil: String? = null
)

@Entity(tableName = "Alerta")
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

data class AlertaConPerfil(
    @Embedded
    val alerta: AlertaEntity,
    val nombrePerfil: String?
)

@Entity(tableName = "Ubicacion")
data class UbicacionEntity(
    @PrimaryKey
    val idUbicacion: String = UUID.randomUUID().toString(),
    val latitud: Double,
    val longitud: Double,
    val fechaHora: Long = System.currentTimeMillis(),
    val idSmartwatch: String
)

data class LatestProfileLocation(
    val idPerfil: String,
    val idUbicacion: String,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: Long,
    val idSmartwatch: String
)
