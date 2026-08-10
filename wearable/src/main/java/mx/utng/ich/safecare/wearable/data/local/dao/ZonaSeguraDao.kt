
package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.ZonaSeguraEntity

@Dao
interface ZonaSeguraDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarZonas(zonas: List<ZonaSeguraEntity>)

    @Query("SELECT * FROM ZonaSegura WHERE idPerfil = :idPerfil AND activa = 1")
    suspend fun obtenerZonasActivas(idPerfil: String): List<ZonaSeguraEntity>

    @Query("SELECT * FROM ZonaSegura WHERE idZona = :idZona LIMIT 1")
    suspend fun obtenerPorId(idZona: String): ZonaSeguraEntity?

    @Query("DELETE FROM ZonaSegura WHERE idPerfil = :idPerfil")
    suspend fun eliminarPorPerfil(idPerfil: String)

    @Query("DELETE FROM ZonaSegura")
    suspend fun eliminarTodas()
}
