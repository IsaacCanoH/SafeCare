package mx.utng.ich.safecare.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.utng.ich.safecare.data.local.entity.*

@Dao
interface PerfilMonitoreadoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(perfil: PerfilMonitoreadoEntity)

    @Delete
    suspend fun eliminar(perfil: PerfilMonitoreadoEntity)

    @Query("SELECT * FROM PerfilMonitoreado WHERE idCuidador = :idCuidador")
    suspend fun obtenerPorCuidador(idCuidador: String): List<PerfilMonitoreadoEntity>
}

@Dao
interface ZonaSeguraDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(zona: ZonaSeguraEntity)

    @Update
    suspend fun actualizar(zona: ZonaSeguraEntity)

    @Delete
    suspend fun eliminar(zona: ZonaSeguraEntity)

    @Query("SELECT * FROM ZonaSegura")
    suspend fun obtenerTodas(): List<ZonaSeguraEntity>

    @Query("SELECT * FROM ZonaSegura WHERE idPerfil = :idPerfil")
    suspend fun obtenerPorPerfil(idPerfil: String): List<ZonaSeguraEntity>
}

@Dao
interface SmartwatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(smartwatch: SmartwatchEntity)

    @Query("SELECT * FROM SmartWatch WHERE idPerfil = :idPerfil LIMIT 1")
    suspend fun obtenerPorPerfil(idPerfil: String): SmartwatchEntity?

    @Query("SELECT * FROM SmartWatch WHERE watchInstallationId = :watchId LIMIT 1")
    suspend fun obtenerPorWatchId(watchId: String): SmartwatchEntity?

    @Query("DELETE FROM SmartWatch WHERE idPerfil = :idPerfil")
    suspend fun eliminarPorPerfil(idPerfil: String)
}

@Dao
interface AlertaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(alerta: AlertaEntity)

    @Query(
        """
        SELECT a.*, p.nombre AS nombrePerfil
        FROM Alerta AS a
        LEFT JOIN PerfilMonitoreado AS p ON p.idPerfil = a.idPerfil
        ORDER BY a.fechaHora DESC
        """
    )
    fun observarTodas(): Flow<List<AlertaConPerfil>>
}

@Dao
interface UbicacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ubicacion: UbicacionEntity)

    @Query("SELECT * FROM Ubicacion ORDER BY fechaHora DESC LIMIT :limit")
    suspend fun obtenerRecientes(limit: Int = 100): List<UbicacionEntity>

    @Query(
        """
        SELECT
            s.idPerfil AS idPerfil,
            u.idUbicacion AS idUbicacion,
            u.latitud AS latitud,
            u.longitud AS longitud,
            u.fechaHora AS fechaHora,
            u.idSmartwatch AS idSmartwatch
        FROM SmartWatch AS s
        INNER JOIN Ubicacion AS u
            ON u.idSmartwatch = s.idSmartwatch
            OR u.idSmartwatch = s.watchInstallationId
        WHERE s.idPerfil IS NOT NULL
          AND u.fechaHora = (
              SELECT MAX(latest.fechaHora)
              FROM Ubicacion AS latest
              WHERE latest.idSmartwatch = u.idSmartwatch
          )
        """
    )
    fun observarUltimasPorPerfil(): Flow<List<LatestProfileLocation>>
}
