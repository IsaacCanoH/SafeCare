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

    @Query("SELECT * FROM perfil_monitoreado WHERE id_perfil = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): PerfilMonitoreadoEntity?

    @Query("SELECT * FROM perfil_monitoreado WHERE estado_actual = 1 LIMIT 1")
    suspend fun obtenerPerfilActivo(): PerfilMonitoreadoEntity?

    @Query("DELETE FROM perfil_monitoreado")
    suspend fun eliminarTodo()
}
