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
        SELECT * FROM alertas
        ORDER BY fechaCreacion DESC
        """
    )
    suspend fun obtenerTodas(): List<AlertaEntity>

    @Query(
        """
        SELECT * FROM alertas
        WHERE sincronizada = 0
        ORDER BY fechaCreacion ASC
        """
    )
    suspend fun obtenerPendientesDeSincronizar(): List<AlertaEntity>

    @Query(
        """
        UPDATE alertas
        SET sincronizada = 1
        WHERE id = :idAlerta
        """
    )
    suspend fun marcarComoSincronizada(idAlerta: Long)

    @Query("DELETE FROM alertas")
    suspend fun eliminarTodas()
}