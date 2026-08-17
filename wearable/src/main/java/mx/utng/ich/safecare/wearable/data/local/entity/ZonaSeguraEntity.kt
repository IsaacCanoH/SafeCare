
package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ZonaSegura")
/**
 * Registro base que almacena los parÃ¡metros exactos de una zona de contenciÃ³n espacial (geocerca).
 *  * Define atributos vitales como las coordenadas del centro, el radio de tolerancia (en metros) y la identidad de la zona.
 */
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