package mx.utng.ich.safecaretv.data.youtube

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.Html
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.time.Duration
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mx.utng.ich.safecaretv.BuildConfig

class YouTubeRepository(
    context: Context,
    private val apiKey: String = BuildConfig.YOUTUBE_API_KEY
) {
    private val appContext = context.applicationContext
    private val client = HttpClient(Android)
    private val json = Json { ignoreUnknownKeys = true }
    private val certificateSha1 = appContext.signingCertificateSha1()

    /** Descarga videos de YouTube recomendados para el cuidado. */
    suspend fun getCareRecommendations(): List<YouTubeVideo> {
        check(apiKey.isNotBlank()) {
            "Falta configurar YOUTUBE_API_KEY en local.properties"
        }

        val searchResponse = client.get("$BASE_URL/search") {
            addAndroidRestrictionHeaders()
            parameter("part", "snippet")
            parameter(
                "q",
                "cuidados para adultos mayores|cuidado salud y seguridad de niños"
            )
            parameter("type", "video")
            parameter("maxResults", MAX_RESULTS)
            parameter("order", "relevance")
            parameter("relevanceLanguage", "es")
            parameter("regionCode", "MX")
            parameter("safeSearch", "strict")
            parameter("key", apiKey)
        }
        val searchBody = searchResponse.bodyAsText()
        ensureSuccessful(searchResponse.status.value, searchBody)

        val root = json.parseToJsonElement(searchBody).jsonObject
        val searchItems = root["items"]?.jsonArray.orEmpty()
        val videosWithoutDuration = searchItems.mapNotNull { element ->
            val item = element.jsonObject
            val id = item["id"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val snippet = item["snippet"]?.jsonObject ?: return@mapNotNull null
            val thumbnail = snippet["thumbnails"]?.jsonObject
                ?.bestThumbnailUrl()
                ?: return@mapNotNull null

            YouTubeVideo(
                id = id,
                title = decodeHtml(snippet.string("title")),
                channelTitle = decodeHtml(snippet.string("channelTitle")),
                thumbnailUrl = thumbnail,
                duration = ""
            )
        }

        if (videosWithoutDuration.isEmpty()) return emptyList()

        val durations = getDurations(videosWithoutDuration.map { it.id })
        return videosWithoutDuration.map { video ->
            video.copy(duration = durations[video.id].orEmpty())
        }
    }

    /** Libera el cliente HTTP usado para consultar YouTube. */
    fun close() {
        client.close()
    }

    /** Consulta y asocia la duración de cada video recomendado. */
    private suspend fun getDurations(videoIds: List<String>): Map<String, String> {
        val response = client.get("$BASE_URL/videos") {
            addAndroidRestrictionHeaders()
            parameter("part", "contentDetails")
            parameter("id", videoIds.joinToString(","))
            parameter("key", apiKey)
        }
        val responseBody = response.bodyAsText()
        ensureSuccessful(response.status.value, responseBody)

        return json.parseToJsonElement(responseBody)
            .jsonObject["items"]
            ?.jsonArray
            .orEmpty()
            .mapNotNull { element ->
                val item = element.jsonObject
                val id = item["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val isoDuration = item["contentDetails"]?.jsonObject
                    ?.get("duration")?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                id to formatDuration(isoDuration)
            }
            .toMap()
    }

    /** Valida que la respuesta HTTP de YouTube sea correcta. */
    private fun ensureSuccessful(statusCode: Int, responseBody: String) {
        if (statusCode in 200..299) return
        val reason = runCatching {
            json.parseToJsonElement(responseBody).jsonObject["error"]
                ?.jsonObject
                ?.get("errors")
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("reason")
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()
        error(
            when (reason) {
                "quotaExceeded", "dailyLimitExceeded" ->
                    "Se agotó temporalmente la cuota de YouTube"
                "keyInvalid", "accessNotConfigured" ->
                    "La API key de YouTube no es válida o la API no está habilitada"
                else -> "YouTube no respondió correctamente (código $statusCode)"
            }
        )
    }

    /** Obtiene un texto obligatorio de un objeto JSON. */
    private fun JsonObject.string(key: String): String =
        get(key)?.jsonPrimitive?.contentOrNull.orEmpty()

    /** Elige la miniatura de mayor calidad disponible. */
    private fun JsonObject.bestThumbnailUrl(): String? =
        listOf("medium", "high", "default")
            .firstNotNullOfOrNull { quality ->
                get(quality)?.jsonObject
                    ?.get("url")?.jsonPrimitive?.contentOrNull
            }

    /** Agrega datos de la app requeridos por la restricción Android. */
    private fun io.ktor.client.request.HttpRequestBuilder.addAndroidRestrictionHeaders() {
        header("X-Android-Package", BuildConfig.APPLICATION_ID)
        if (certificateSha1.isNotBlank()) {
            header("X-Android-Cert", certificateSha1)
        }
    }

    @Suppress("DEPRECATION")
    /** Decodifica entidades HTML presentes en los títulos de video. */
    private fun decodeHtml(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()

    /** Convierte la duración ISO de YouTube a un formato legible. */
    private fun formatDuration(value: String): String = runCatching {
        val duration = Duration.parse(value)
        val totalSeconds = duration.seconds
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "$minutes:${seconds.toString().padStart(2, '0')}"
        }
    }.getOrDefault("")

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3"
        private const val MAX_RESULTS = 6
    }
}

/** Obtiene la huella SHA-1 de firma para las solicitudes de YouTube. */
private fun Context.signingCertificateSha1(): String = runCatching {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        ).signingInfo?.apkContentsSigners
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
    }
    val certificate = signatures?.firstOrNull()?.toByteArray() ?: return@runCatching ""
    MessageDigest.getInstance("SHA-1")
        .digest(certificate)
        .joinToString("") { byte -> "%02X".format(byte) }
}.getOrDefault("")
