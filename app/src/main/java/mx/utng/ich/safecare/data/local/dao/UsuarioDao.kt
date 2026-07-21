package mx.utng.ich.safecare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.data.local.entity.UsuarioEntity

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registrar(usuario: UsuarioEntity)

    @Query("SELECT * FROM Usuario WHERE correo = :correo LIMIT 1")
    suspend fun obtenerPorCorreo(correo: String): UsuarioEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM Usuario WHERE correo = :correo)")
    suspend fun existeCorreo(correo: String): Boolean
}
