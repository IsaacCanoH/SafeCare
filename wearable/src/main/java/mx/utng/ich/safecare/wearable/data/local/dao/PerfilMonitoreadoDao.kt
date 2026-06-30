package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.PerfilMonitoreadoEntity

@Dao
interface PerfilMonitoreadoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(perfil: PerfilMonitoreadoEntity)

    @Query("SELECT * FROM PerfilMonitoreado WHERE idPerfil = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): PerfilMonitoreadoEntity?

    @Query("SELECT * FROM PerfilMonitoreado WHERE estadoActual = 1 LIMIT 1")
    suspend fun obtenerPerfilActivo(): PerfilMonitoreadoEntity?

    @Query("DELETE FROM PerfilMonitoreado")
    suspend fun eliminarTodo()
}
