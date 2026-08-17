package mx.utng.ich.safecaretv.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Componente de audio encargado de la reproducciÃ³n de tonos de alerta en el dispositivo mÃ³vil.
 *  * Maneja los ciclos de vida de reproducciÃ³n del sonido, asegurando que las alarmas crÃ­ticas suenen a los niveles de volumen adecuados, incluso en modos restrictivos.
 */
class AlertTonePlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    /** Reproduce una vista previa corta del tono seleccionado. */
    fun playPreview(tone: AlertTone) {
        stop()
        mediaPlayer = createPlayer(tone.soundResId).apply {
            isLooping = false
            setOnCompletionListener {
                it.release()
                if (mediaPlayer === it) mediaPlayer = null
            }
            start()
        }
    }

    /** Reproduce el tono configurado para una alerta activa. */
    fun playAlert(tone: AlertTone) {
        stop()
        mediaPlayer = createPlayer(tone.soundResId).apply {
            isLooping = true
            start()
        }
    }

    /** Detiene y libera el reproductor de audio actual. */
    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    /** Crea un reproductor de audio con el recurso indicado. */
    private fun createPlayer(soundResId: Int): MediaPlayer {
        val descriptor = context.resources.openRawResourceFd(soundResId)
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            descriptor.close()
            prepare()
        }
    }
}