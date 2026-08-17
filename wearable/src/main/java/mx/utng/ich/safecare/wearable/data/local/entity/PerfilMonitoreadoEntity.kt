
package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PerfilMonitoreado")
/**
 * Entidad relacional para mapear los detalles de los perfiles monitoreados en las tablas de SQLite (Room).
 *  * Define el esquema, las restricciones y las relaciones de los datos del paciente en el almacenamiento persistente local.
 */
data class PerfilMonitoreadoEntity(
    @PrimaryKey
    val idPerfil: String,
    val nombre: String,
    val edad: Int,
    val fechaNacimiento: String? = null,
    val tipoPerfil: String,
    val foto: String? = null,
    val estadoActual: Boolean,
    val idCuidador: String
)