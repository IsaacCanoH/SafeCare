package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity

@Dao
interface AlertaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(alerta: AlertaEntity): Long

    @Query(
        """
        SELECT * FROM Alertas
        ORDER BY fechaHora DESC
        """
    )
    suspend fun obtenerTodas(): List<AlertaEntity>

    @Query("DELETE FROM Alertas")
    suspend fun eliminarTodas()
}
