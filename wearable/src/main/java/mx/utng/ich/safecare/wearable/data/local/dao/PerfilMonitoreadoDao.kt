
package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.PerfilMonitoreadoEntity

@Dao
/**
 * DAO especializado para leer, insertar y actualizar los perfiles mÃ©dicos y de pacientes en el repositorio local.
 *  * Define las consultas SQL Ã³ptimas para la gestiÃ³n de las identidades de las personas bajo el cuidado del sistema.
 */
interface PerfilMonitoreadoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /** Guarda o actualiza un perfil monitoreado local. */
    suspend fun insertar(perfil: PerfilMonitoreadoEntity)

    @Query("SELECT * FROM PerfilMonitoreado WHERE idPerfil = :id LIMIT 1")
    /** Busca un perfil local por su identificador. */
    suspend fun obtenerPorId(id: String): PerfilMonitoreadoEntity?

    @Query("SELECT * FROM PerfilMonitoreado WHERE estadoActual = 1 LIMIT 1")
    /** Obtiene el perfil marcado como activo en el reloj. */
    suspend fun obtenerPerfilActivo(): PerfilMonitoreadoEntity?

    @Query("UPDATE PerfilMonitoreado SET estadoActual = 0")
    /** Desactiva todos los perfiles almacenados localmente. */
    suspend fun desactivarTodos()

    @Query("DELETE FROM PerfilMonitoreado WHERE idPerfil = :idPerfil")
    /** Elimina el perfil local con el identificador indicado. */
    suspend fun eliminarPorId(idPerfil: String)

    @Query("DELETE FROM PerfilMonitoreado")
    /** Elimina todos los perfiles almacenados en el reloj. */
    suspend fun eliminarTodo()
}