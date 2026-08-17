
package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity

@Dao
/**
 * Clase DAO responsable de la lectura, escritura y depuraciÃ³n del historial de trayectorias GPS.
 *  * Permite almacenar localmente las rutas seguidas por el dispositivo cuando la conexiÃ³n a la nube no estÃ¡ disponible.
 */
interface UbicacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    /** Guarda una ubicación en la base local del reloj. */
    suspend fun insertar(ubicacion: UbicacionEntity): Long

    @Query("SELECT * FROM Ubicacion ORDER BY fechaHora DESC")
    /** Obtiene las ubicaciones guardadas localmente. */
    suspend fun obtenerTodas(): List<UbicacionEntity>

    @Query(
        """
        DELETE FROM Ubicacion
        WHERE idUbicacion NOT IN (
            SELECT idUbicacion FROM Ubicacion
            ORDER BY fechaHora DESC
            LIMIT :maxRecords
        )
        """
    )
    /** Conserva solo las ubicaciones locales más recientes. */
    suspend fun conservarSoloRegistrosRecientes(maxRecords: Int)

    @Query("DELETE FROM Ubicacion")
    /** Elimina todas las ubicaciones guardadas en el reloj. */
    suspend fun eliminarTodas()
}