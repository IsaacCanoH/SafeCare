package mx.utng.ich.safecare.wearable.data.model

enum class TipoPerfil {
    MENOR,
    ADULTO_MAYOR,
    CUIDADOR
}

enum class EstadoAlerta {
    ACTIVA,
    ATENDIDA,
    FALSA_ALARMA
}

enum class TipoAlerta {
    SOS,
    ZONA_SEGURA,
    BATERIA_BAJA,
    SIN_CONEXION
}

enum class TipoConexion {
    ONLINE,
    OFFLINE
}
