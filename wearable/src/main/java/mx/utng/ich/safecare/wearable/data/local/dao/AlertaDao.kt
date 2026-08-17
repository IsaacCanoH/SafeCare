
package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity

@Dao
/**
 * Objeto de Acceso a Datos (DAO) para las operaciones de base de datos relacionadas con las alertas del sistema.
 *  * Permite la persistencia, consulta, actualizaciÃ³n y eliminaciÃ³n de los registros de incidentes.
 *  * Facilita el acceso estructurado a los historiales para auditorÃ­as o anÃ¡lisis de patrones de riesgo.
 */
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