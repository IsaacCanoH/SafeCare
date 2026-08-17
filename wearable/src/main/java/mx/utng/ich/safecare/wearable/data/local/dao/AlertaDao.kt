
package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity

@Dao
interface AlertaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /** Guarda una alerta en el almacenamiento local. */
    suspend fun insertar(alerta: AlertaEntity): Long

    @Query(
        """
        SELECT * FROM Alertas
        ORDER BY fechaHora DESC
        """
    )
    /** Obtiene todas las alertas almacenadas en el reloj. */
    suspend fun obtenerTodas(): List<AlertaEntity>

    @Query("DELETE FROM Alertas")
    /** Elimina todas las alertas almacenadas localmente. */
    suspend fun eliminarTodas()
}
