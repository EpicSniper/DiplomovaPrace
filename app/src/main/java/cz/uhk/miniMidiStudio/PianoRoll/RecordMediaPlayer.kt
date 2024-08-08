package cz.uhk.miniMidiStudio.PianoRoll

import android.content.Context
import android.media.MediaPlayer
import cz.uhk.miniMidiStudio.Project.Track
import java.io.File
import java.io.IOException

class RecordMediaPlayer (private val track: Track) {
    private val mediaPlayer = MediaPlayer()
    private var isPlaying: Boolean = false
    private var playingStarted: Boolean = false

    public fun stopPlaying() {
        if (isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
            isPlaying = false
        }
    }

    public fun startPlaying(context: Context, startTimeInMiliSeconds: Int) {
        try {
            val file = File(context.filesDir, "${track.getAudioFile()}.mp3")
            val absolutePath = file.absolutePath
            mediaPlayer.setDataSource(absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.seekTo(startTimeInMiliSeconds)
            mediaPlayer.start()
            isPlaying = true
            playingStarted = true
            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
                isPlaying = false
            }
        } catch (_: IOException) {
            isPlaying = false
            playingStarted = false
        } catch (_: IllegalStateException) {
            isPlaying = false
            playingStarted = false
        }
    }

    public fun hasPlayingStarted(): Boolean {
        return playingStarted
    }
}