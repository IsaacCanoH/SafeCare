
package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "Alertas")
/**
 * Entidad de base de datos que representa una alerta o incidente generado en el sistema.
 *  * Almacena informaciÃ³n crÃ­tica como el tipo de alerta, el nivel de gravedad, la fecha y hora exacta, y el perfil afectado.
 *  * Se utiliza en conjunciÃ³n con Room para garantizar la persistencia local de los datos.
 */
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