
package mx.utng.ich.safecare.wearable.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mx.utng.ich.safecare.wearable.data.local.entity.ZonaSeguraEntity

@Dao
interface ZonaSeguraDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    // Guarda el conjunto de zonas seguras sincronizadas.
    suspend fun insertarZonas(zonas: List<ZonaSeguraEntity>)

    @Query("SELECT * FROM ZonaSegura WHERE idPerfil = :idPerfil AND activa = 1")
    // Obtiene las zonas activas del perfil indicado.
    suspend fun obtenerZonasActivas(idPerfil: String): List<ZonaSeguraEntity>

    @Query("SELECT * FROM ZonaSegura WHERE idZona = :idZona LIMIT 1")
    // Busca una zona segura local por su identificador.
    suspend fun obtenerPorId(idZona: String): ZonaSeguraEntity?

    @Query("DELETE FROM ZonaSegura WHERE idPerfil = :idPerfil")
    // Elimina las zonas seguras de un perfil local.
    suspend fun eliminarPorPerfil(idPerfil: String)

    @Query("DELETE FROM ZonaSegura")
    // Elimina todas las zonas seguras almacenadas.
    suspend fun eliminarTodas()
}
