package mx.utng.ich.safecare.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = false
)
abstract class SafeCareAppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun perfilMonitoreadoDao(): PerfilMonitoreadoDao
    abstract fun zonaSeguraDao(): ZonaSeguraDao
    abstract fun smartwatchDao(): SmartwatchDao
    abstract fun alertaDao(): AlertaDao
    abstract fun ubicacionDao(): UbicacionDao

    companion object {
        @Volatile
        private var INSTANCE: SafeCareAppDatabase? = null

        fun getDatabase(context: Context): SafeCareAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeCareAppDatabase::class.java,
                    "safecare_app_db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `SmartWatch` ADD COLUMN `watchInstallationId` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `SmartWatch` ADD COLUMN `nombreDispositivo` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `SmartWatch` ADD COLUMN `modelo` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `SmartWatch` ADD COLUMN `dataLayerNodeId` TEXT"
                )
                db.execSQL(
                    """
                    UPDATE `SmartWatch`
                    SET `watchInstallationId` = `numeroSerie`
                    WHERE `watchInstallationId` IS NULL
                    """.trimIndent()
                )
            }
        }
    }
}
