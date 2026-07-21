package mx.utng.ich.safecare.data.local.dao

import androidx.room.*
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
}
