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

    @Query("SELECT * FROM Ubicacion ORDER BY fechaHora DESC")
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
    suspend fun conservarSoloRegistrosRecientes(maxRecords: Int)

    @Query("DELETE FROM Ubicacion")
    suspend fun eliminarTodas()
}
