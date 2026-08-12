
package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity

@Dao
interface SmartwatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    // Guarda o actualiza el estado local del smartwatch.
    suspend fun insertarOActualizar(smartwatch: SmartwatchEntity): Long

    @Query("SELECT * FROM SmartWatch ORDER BY ultimaConexion DESC, idSmartwatch DESC LIMIT 1")
    // Obtiene el último estado registrado del smartwatch.
    suspend fun obtenerEstado(): SmartwatchEntity?

    @Query(
        """
        SELECT * FROM SmartWatch
        WHERE numeroSerie = :numeroSerie
        ORDER BY ultimaConexion DESC, idSmartwatch DESC
        LIMIT 1
        """
    )
    // Busca un smartwatch local por su número de serie.
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
    // Conserva solo los estados de smartwatch más recientes.
    suspend fun conservarSoloRegistrosRecientes(maxRecords: Int)
}
