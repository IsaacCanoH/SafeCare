package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity

@Dao
interface UbicacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ubicacion: UbicacionEntity): Long

    @Query("SELECT * FROM ubicaciones ORDER BY fechaHora DESC")
    suspend fun obtenerTodas(): List<UbicacionEntity>

    @Query("SELECT * FROM ubicaciones WHERE sincronizada = 0 ORDER BY fechaHora ASC")
    suspend fun obtenerPendientesDeSincronizar(): List<UbicacionEntity>

    @Query("UPDATE ubicaciones SET sincronizada = 1 WHERE id = :idUbicacion")
    suspend fun marcarComoSincronizada(idUbicacion: Long)

    @Query(
        """
        DELETE FROM ubicaciones
        WHERE id NOT IN (
            SELECT id FROM ubicaciones
            ORDER BY fechaHora DESC
            LIMIT :maxRecords
        )
        """
    )
    suspend fun conservarSoloRegistrosRecientes(maxRecords: Int)

    @Query("DELETE FROM ubicaciones")
    suspend fun eliminarTodas()
}
