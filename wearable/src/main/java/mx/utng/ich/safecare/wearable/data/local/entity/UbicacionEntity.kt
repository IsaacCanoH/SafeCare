
package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "Ubicacion")
/**
 * Estructura de entidad que encapsula un punto geogrÃ¡fico (latitud, longitud) y un timestamp.
 *  * Forma el nÃºcleo de la traza de movimiento del usuario monitoreado para su posterior anÃ¡lisis.
 */
data class UbicacionEntity(
    @PrimaryKey
    val idUbicacion: String = UUID.randomUUID().toString(),
    val latitud: Double,
    val longitud: Double,
    val fechaHora: Long = System.currentTimeMillis(),
    val idSmartwatch: String
)