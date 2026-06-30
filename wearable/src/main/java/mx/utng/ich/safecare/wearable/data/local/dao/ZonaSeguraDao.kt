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

    @Query("DELETE FROM ZonaSegura")
    suspend fun eliminarTodas()
}
