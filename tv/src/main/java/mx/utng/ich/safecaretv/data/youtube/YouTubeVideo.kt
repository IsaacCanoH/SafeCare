package mx.utng.ich.safecaretv.data.youtube

/**
 * RepresentaciÃ³n estructural rica de un video de YouTube integrado en la plataforma.
 *  * Incluye identificadores de video, metadatos descriptivos, imÃ¡genes de miniaturas de alta resoluciÃ³n y tiempos de duraciÃ³n.
 */
data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val duration: String
) {
    val watchUrl: String
        get() = "https://www.youtube.com/watch?v=$id"
}