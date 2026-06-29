package mx.utng.ich.safecare.wearable.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    @Volatile
    private var instance: SafeCareDatabase? = null

    fun getDatabase(context: Context): SafeCareDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SafeCareDatabase::class.java,
                "safecare_database"
            )
                .addMigrations(MIGRATION_8_10, MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                .also { database ->
                    instance = database
                }
        }
    }

    private val MIGRATION_8_10 = object : Migration(8, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateSmartwatchTable(db, copyTemporaryHistory = false)
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateSmartwatchTable(db, copyTemporaryHistory = true)
        }
    }

    private fun migrateSmartwatchTable(
        db: SupportSQLiteDatabase,
        copyTemporaryHistory: Boolean
    ) {
        db.execSQL("ALTER TABLE `smartwatch` RENAME TO `smartwatch_old`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `smartwatch` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `numeroSerie` TEXT NOT NULL,
                `bateria` INTEGER NOT NULL,
                `conexion` TEXT NOT NULL,
                `ultimaConexion` INTEGER NOT NULL,
                `motivo` TEXT NOT NULL,
                `idPerfil` TEXT,
                `sincronizado` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `smartwatch` (
                `numeroSerie`,
                `bateria`,
                `conexion`,
                `ultimaConexion`,
                `motivo`,
                `idPerfil`,
                `sincronizado`
            )
            SELECT
                `numeroSerie`,
                `bateria`,
                `conexion`,
                `ultimaConexion`,
                'MIGRACION',
                `idPerfil`,
                `sincronizado`
            FROM `smartwatch_old`
            """.trimIndent()
        )

        if (copyTemporaryHistory) {
            db.execSQL(
                """
                INSERT INTO `smartwatch` (
                    `numeroSerie`,
                    `bateria`,
                    `conexion`,
                    `ultimaConexion`,
                    `motivo`,
                    `idPerfil`,
                    `sincronizado`
                )
                SELECT
                    `numeroSerie`,
                    `bateria`,
                    `conexion`,
                    `fechaHora`,
                    `motivo`,
                    `idPerfil`,
                    `sincronizado`
                FROM `smartwatch_estado_historial`
                """.trimIndent()
            )
        }

        db.execSQL("DROP TABLE `smartwatch_old`")
        db.execSQL("DROP TABLE IF EXISTS `smartwatch_estado_historial`")
    }
}
