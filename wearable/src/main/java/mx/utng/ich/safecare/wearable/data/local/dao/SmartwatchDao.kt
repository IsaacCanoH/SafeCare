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

    @Query("SELECT * FROM SmartWatch ORDER BY ultimaConexion DESC, idSmartwatch DESC LIMIT 1")
    suspend fun obtenerEstado(): SmartwatchEntity?

    @Query(
        """
        SELECT * FROM SmartWatch
        WHERE numeroSerie = :numeroSerie
        ORDER BY ultimaConexion DESC, idSmartwatch DESC
        LIMIT 1
        """
    )
    suspend fun obtenerPorNumeroSerie(numeroSerie: String): SmartwatchEntity?

    @Query(
        """
        DELETE FROM SmartWatch
        WHERE idSmartwatch NOT IN (
            SELECT idSmartwatch FROM SmartWatch
            ORDER BY ultimaConexion DESC, idSmartwatch DESC
            LIMIT :maxRecords
        )
        """
    )
    suspend fun conservarSoloRegistrosRecientes(maxRecords: Int)
}
