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
                .addMigrations(MIGRATION_8_10, MIGRATION_9_10, MIGRATION_10_11)
                .fallbackToDestructiveMigration(dropAllTables = true)
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

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migratePerfilMonitoreadoToModel(db)
            migrateZonaSeguraToModel(db)
            migrateSmartwatchToModel(db)
            migrateUbicacionToModel(db)
            migrateAlertasToModel(db)
        }
    }

    private fun migrateSmartwatchTable(
        db: SupportSQLiteDatabase,
        copyTemporaryHistory: Boolean
    ) {
        if (!tableExists(db, "smartwatch")) {
            return
        }

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

        if (copyTemporaryHistory && tableExists(db, "smartwatch_estado_historial")) {
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

    private fun migratePerfilMonitoreadoToModel(db: SupportSQLiteDatabase) {
        db.execSQL(createPerfilMonitoreadoSql())

        if (!tableExists(db, "perfil_monitoreado")) {
            return
        }

        db.execSQL(
            """
            INSERT OR REPLACE INTO `PerfilMonitoreado` (
                `idPerfil`,
                `nombre`,
                `edad`,
                `fechaNacimiento`,
                `tipoPerfil`,
                `foto`,
                `estadoActual`,
                `idCuidador`
            )
            SELECT
                `id_perfil`,
                `nombre`,
                `edad`,
                `fecha_nacimiento`,
                `tipo_perfil`,
                `foto_url`,
                `estado_actual`,
                `id_cuidador`
            FROM `perfil_monitoreado`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `perfil_monitoreado`")
    }

    private fun migrateZonaSeguraToModel(db: SupportSQLiteDatabase) {
        db.execSQL(createZonaSeguraSql())

        if (!tableExists(db, "zona_segura")) {
            return
        }

        db.execSQL(
            """
            INSERT OR REPLACE INTO `ZonaSegura` (
                `idZona`,
                `nombre`,
                `latitudCentro`,
                `longitudCentro`,
                `radioMetros`,
                `activa`,
                `idPerfil`
            )
            SELECT
                `id_zona`,
                `nombre`,
                `latitud_centro`,
                `longitud_centro`,
                `radio_metros`,
                `activa`,
                `id_perfil`
            FROM `zona_segura`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `zona_segura`")
    }

    private fun migrateSmartwatchToModel(db: SupportSQLiteDatabase) {
        if (tableExists(db, "smartwatch")) {
            db.execSQL("ALTER TABLE `smartwatch` RENAME TO `smartwatch_model_old`")
        }

        db.execSQL(createSmartwatchSql())

        if (!tableExists(db, "smartwatch_model_old")) {
            return
        }

        db.execSQL(
            """
            INSERT OR REPLACE INTO `SmartWatch` (
                `idSmartwatch`,
                `numeroSerie`,
                `bateria`,
                `conexion`,
                `ultimaConexion`,
                `estado`,
                `idPerfil`
            )
            SELECT
                COALESCE(NULLIF(`numeroSerie`, ''), CAST(`id` AS TEXT)),
                `numeroSerie`,
                `bateria`,
                `conexion`,
                `ultimaConexion`,
                CASE
                    WHEN LOWER(`conexion`) = 'online' THEN 'ACTIVO'
                    ELSE 'INACTIVO'
                END,
                `idPerfil`
            FROM `smartwatch_model_old`
            ORDER BY `ultimaConexion` ASC, `id` ASC
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `smartwatch_model_old`")
    }

    private fun migrateUbicacionToModel(db: SupportSQLiteDatabase) {
        db.execSQL(createUbicacionSql())

        if (!tableExists(db, "ubicaciones")) {
            return
        }

        db.execSQL(
            """
            INSERT OR REPLACE INTO `Ubicacion` (
                `idUbicacion`,
                `latitud`,
                `longitud`,
                `fechaHora`,
                `idSmartwatch`
            )
            SELECT
                CAST(`id` AS TEXT),
                `latitud`,
                `longitud`,
                `fechaHora`,
                `idSmartwatch`
            FROM `ubicaciones`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `ubicaciones`")
    }

    private fun migrateAlertasToModel(db: SupportSQLiteDatabase) {
        if (tableExists(db, "alertas")) {
            db.execSQL("ALTER TABLE `alertas` RENAME TO `alertas_model_old`")
        }

        db.execSQL(createAlertasSql())

        if (!tableExists(db, "alertas_model_old")) {
            return
        }

        db.execSQL(
            """
            INSERT OR REPLACE INTO `Alertas` (
                `idAlerta`,
                `tipoAlerta`,
                `descripcion`,
                `fechaHora`,
                `estado`,
                `idPerfil`,
                `idUbicacion`
            )
            SELECT
                CAST(`id` AS TEXT),
                `tipoAlerta`,
                `descripcion`,
                `fechaCreacion`,
                'ACTIVA',
                `idPerfil`,
                `idUbicacion`
            FROM `alertas_model_old`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `alertas_model_old`")
    }

    private fun createPerfilMonitoreadoSql(): String {
        return """
            CREATE TABLE IF NOT EXISTS `PerfilMonitoreado` (
                `idPerfil` TEXT NOT NULL,
                `nombre` TEXT NOT NULL,
                `edad` INTEGER NOT NULL,
                `fechaNacimiento` TEXT,
                `tipoPerfil` TEXT NOT NULL,
                `foto` TEXT,
                `estadoActual` INTEGER NOT NULL,
                `idCuidador` TEXT NOT NULL,
                PRIMARY KEY(`idPerfil`)
            )
        """.trimIndent()
    }

    private fun createZonaSeguraSql(): String {
        return """
            CREATE TABLE IF NOT EXISTS `ZonaSegura` (
                `idZona` TEXT NOT NULL,
                `nombre` TEXT NOT NULL,
                `latitudCentro` REAL NOT NULL,
                `longitudCentro` REAL NOT NULL,
                `radioMetros` REAL NOT NULL,
                `activa` INTEGER NOT NULL,
                `idPerfil` TEXT NOT NULL,
                PRIMARY KEY(`idZona`)
            )
        """.trimIndent()
    }

    private fun createSmartwatchSql(): String {
        return """
            CREATE TABLE IF NOT EXISTS `SmartWatch` (
                `idSmartwatch` TEXT NOT NULL,
                `numeroSerie` TEXT NOT NULL,
                `bateria` INTEGER NOT NULL,
                `conexion` TEXT NOT NULL,
                `ultimaConexion` INTEGER NOT NULL,
                `estado` TEXT NOT NULL,
                `idPerfil` TEXT,
                PRIMARY KEY(`idSmartwatch`)
            )
        """.trimIndent()
    }

    private fun createUbicacionSql(): String {
        return """
            CREATE TABLE IF NOT EXISTS `Ubicacion` (
                `idUbicacion` TEXT NOT NULL,
                `latitud` REAL NOT NULL,
                `longitud` REAL NOT NULL,
                `fechaHora` INTEGER NOT NULL,
                `idSmartwatch` TEXT NOT NULL,
                PRIMARY KEY(`idUbicacion`)
            )
        """.trimIndent()
    }

    private fun createAlertasSql(): String {
        return """
            CREATE TABLE IF NOT EXISTS `Alertas` (
                `idAlerta` TEXT NOT NULL,
                `tipoAlerta` TEXT NOT NULL,
                `descripcion` TEXT NOT NULL,
                `fechaHora` INTEGER NOT NULL,
                `estado` TEXT NOT NULL,
                `idPerfil` TEXT NOT NULL,
                `idUbicacion` TEXT,
                PRIMARY KEY(`idAlerta`)
            )
        """.trimIndent()
    }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        )
        cursor.use {
            return it.moveToFirst()
        }
    }
}
