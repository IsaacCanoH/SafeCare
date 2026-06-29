package mx.utng.ich.safecare.wearable.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import mx.utng.ich.safecare.wearable.data.local.dao.AlertaDao
import mx.utng.ich.safecare.wearable.data.local.dao.UbicacionDao
import mx.utng.ich.safecare.wearable.data.local.dao.SmartwatchDao
import mx.utng.ich.safecare.wearable.data.local.dao.ZonaSeguraDao
import mx.utng.ich.safecare.wearable.data.local.dao.PerfilMonitoreadoDao
import mx.utng.ich.safecare.wearable.data.local.entity.AlertaEntity
import mx.utng.ich.safecare.wearable.data.local.entity.UbicacionEntity
import mx.utng.ich.safecare.wearable.data.local.entity.SmartwatchEntity
import mx.utng.ich.safecare.wearable.data.local.entity.ZonaSeguraEntity
import mx.utng.ich.safecare.wearable.data.local.entity.PerfilMonitoreadoEntity

@Database(
    entities = [
        AlertaEntity::class,
        UbicacionEntity::class,
        SmartwatchEntity::class,
        ZonaSeguraEntity::class,
        PerfilMonitoreadoEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class SafeCareDatabase : RoomDatabase() {

    abstract fun alertaDao(): AlertaDao
    abstract fun ubicacionDao(): UbicacionDao
    abstract fun smartwatchDao(): SmartwatchDao
    abstract fun zonaSeguraDao(): ZonaSeguraDao
    abstract fun perfilMonitoreadoDao(): PerfilMonitoreadoDao
}
