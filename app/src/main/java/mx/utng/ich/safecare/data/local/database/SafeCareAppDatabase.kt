package mx.utng.ich.safecare.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import mx.utng.ich.safecare.data.local.dao.*
import mx.utng.ich.safecare.data.local.entity.*

@Database(
    entities = [
        UsuarioEntity::class,
        PerfilMonitoreadoEntity::class,
        ZonaSeguraEntity::class,
        SmartwatchEntity::class,
        AlertaEntity::class,
        UbicacionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SafeCareAppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun perfilMonitoreadoDao(): PerfilMonitoreadoDao
    abstract fun zonaSeguraDao(): ZonaSeguraDao
    abstract fun smartwatchDao(): SmartwatchDao

    companion object {
        @Volatile
        private var INSTANCE: SafeCareAppDatabase? = null

        fun getDatabase(context: Context): SafeCareAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeCareAppDatabase::class.java,
                    "safecare_app_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
