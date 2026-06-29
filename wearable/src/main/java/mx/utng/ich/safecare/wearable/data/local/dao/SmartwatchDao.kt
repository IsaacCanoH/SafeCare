package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity

@Dao
interface SmartwatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOActualizar(smartwatch: SmartwatchEntity): Long

    @Query("SELECT * FROM smartwatch ORDER BY ultimaConexion DESC, id DESC LIMIT 1")
    suspend fun obtenerEstado(): SmartwatchEntity?

    @Query(
        """
        SELECT * FROM smartwatch
        WHERE numeroSerie = :numeroSerie
        ORDER BY ultimaConexion DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun obtenerPorNumeroSerie(numeroSerie: String): SmartwatchEntity?

    @Query("SELECT * FROM smartwatch WHERE sincronizado = 0 ORDER BY ultimaConexion ASC")
    suspend fun obtenerPendientesDeSincronizar(): List<SmartwatchEntity>

    @Query("UPDATE smartwatch SET sincronizado = 1 WHERE id = :idSmartwatch")
    suspend fun marcarComoSincronizado(idSmartwatch: Long)

    @Query(
        """
        DELETE FROM smartwatch
        WHERE id NOT IN (
            SELECT id FROM smartwatch
            ORDER BY ultimaConexion DESC, id DESC
            LIMIT :maxRecords
        )
        """
    )
    suspend fun conservarSoloRegistrosRecientes(maxRecords: Int)
}
