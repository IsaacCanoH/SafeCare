
package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "SmartWatch")
/**
 * Entidad de modelo relacional que representa fÃ­sicamente un reloj Wear OS asociado a un perfil.
 *  * Guarda parÃ¡metros tÃ©cnicos como la direcciÃ³n MAC, nombre del dispositivo y tokens de vinculaciÃ³n en la base de datos local.
 */
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