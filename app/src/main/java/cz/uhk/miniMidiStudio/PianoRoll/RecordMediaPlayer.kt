package cz.uhk.miniMidiStudio.PianoRoll

import android.content.Context
import android.media.MediaPlayer
import cz.uhk.miniMidiStudio.Project.Track
import java.io.File
import java.io.IOException

class RecordMediaPlayer (private val track: Track) {
    private val mediaPlayer = MediaPlayer()
    private var isPlaying: Boolean = false

    public fun stopPlaying() {
        if (isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
            isPlaying = false
        }
    }

    public fun startPlaying(context: Context, startTimeInSeconds: Int) {
        try {
            val file = File(context.filesDir, "${track.getAudioFile()}.mp3")
            val absolutePath = file.absolutePath
            mediaPlayer.setDataSource(absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.seekTo(startTimeInSeconds * 1000)
            mediaPlayer.start()
            isPlaying = true
            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
                isPlaying = false
            }
        } catch (_: IOException) {
            isPlaying = false
        } catch (_: IllegalStateException) {
            isPlaying = false
        }
    }
}