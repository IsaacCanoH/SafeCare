package mx.utng.ich.safecare.util

import java.security.MessageDigest

/**
 * Utilidades de seguridad para la gestión de credenciales en SafeCare.
 *
 * Centraliza las operaciones criptográficas necesarias para proteger
 * las contraseñas de los usuarios antes de almacenarlas en la base de datos.
 */
object SecurityUtils {
    /**
     * Genera un hash SHA-256 de la contraseña proporcionada.
     *
     * Convierte la contraseña en texto plano a su representación hexadecimal
     * SHA-256 para almacenarla de forma segura en el registro de usuario.
     *
     * @param password Contraseña en texto plano a proteger.
     * @return Cadena hexadecimal del hash SHA-256 de la contraseña.
     */
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
