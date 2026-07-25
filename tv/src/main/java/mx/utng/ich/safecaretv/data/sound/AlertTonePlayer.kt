package mx.utng.ich.safecaretv.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

class AlertTonePlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

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

    fun playAlert(tone: AlertTone) {
        stop()
        mediaPlayer = createPlayer(tone.soundResId).apply {
            isLooping = true
            start()
        }
    }

    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

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
