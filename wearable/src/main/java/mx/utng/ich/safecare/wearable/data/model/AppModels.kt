package mx.utng.ich.safecare.wearable.data.model

/**
 * EnumeraciÃ³n TipoPerfil del sistema SafeCare.
 */
enum class TipoPerfil {
    MENOR,
    ADULTO_MAYOR,
    CUIDADOR
}

/**
 * EnumeraciÃ³n EstadoAlerta del sistema SafeCare.
 */
enum class EstadoAlerta {
    ACTIVA,
    ATENDIDA,
    FALSA_ALARMA
}

/**
 * EnumeraciÃ³n TipoAlerta del sistema SafeCare.
 */
enum class TipoAlerta {
    SOS,
    ZONA_SEGURA,
    BATERIA_BAJA,
    SIN_CONEXION
}

/**
 * EnumeraciÃ³n TipoConexion del sistema SafeCare.
 */
enum class TipoConexion {
    ONLINE,
    OFFLINE
}