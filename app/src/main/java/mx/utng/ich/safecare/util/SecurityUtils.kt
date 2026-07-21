package mx.utng.ich.safecare.util

import java.security.MessageDigest

object SecurityUtils {
    /**
     * Genera un hash SHA-256 de una cadena de texto.
     */
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
