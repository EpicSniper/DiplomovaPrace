package cz.uhk.diplomovaprace.PianoRoll

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.os.HandlerCompat.postDelayed
import cz.uhk.diplomovaprace.Project.Track
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