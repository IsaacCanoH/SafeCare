package mx.utng.ich.safecare.wearable.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perfil_monitoreado")
data class PerfilMonitoreadoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_perfil")
    val idPerfil: String,
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "edad")
    val edad: Int,
    @ColumnInfo(name = "fecha_nacimiento")
    val fechaNacimiento: String? = null,
    @ColumnInfo(name = "tipo_perfil")
    val tipoPerfil: String,
    @ColumnInfo(name = "foto_url")
    val fotoUrl: String? = null,
    @ColumnInfo(name = "estado_actual")
    val estadoActual: Boolean,
    @ColumnInfo(name = "id_cuidador")
    val idCuidador: String
)
