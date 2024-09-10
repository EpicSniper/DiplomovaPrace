package cz.uhk.miniMidiStudio.PianoRoll

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import cz.uhk.miniMidiStudio.Project.Track
import java.io.File
import java.io.IOException

class AudioPlayer (private val track: Track) {
    private lateinit var audioTrack: AudioTrack
    private var isPlaying: Boolean = false
    private var playingStarted: Boolean = false
    private var thread = Thread()
    private var isRunning = false

    public fun stopPlaying() {
        if (isPlaying) {
            isRunning = false
            audioTrack.stop()
            thread.join()
            audioTrack.release()
            isPlaying = false
        }
    }

    public fun startPlaying(startTimeInMiliSeconds: Int) {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack.play()

        val audioData = track.getRecordedAudio()!!
        isRunning = true

        thread = Thread {
            val bytesPerMiliSecond = audioTrack.sampleRate * audioTrack.channelCount * 2 / 1000  // 2 byty na vzorek pro 16-bit PCM
            val startOffset = startTimeInMiliSeconds * bytesPerMiliSecond
            var offset = startOffset.toInt()
            println(offset)
            println(bytesPerMiliSecond)
            println(audioData.size)
            while (isRunning && offset < audioData.size) {
                val bytesWritten = audioTrack.write(audioData, offset, audioData.size - offset)
                offset += bytesWritten
                println(offset)
            }

            audioTrack.stop()
            audioTrack.release()
            isPlaying = false
        }

        thread.start()
        isPlaying = true
        playingStarted = true
    }

    public fun hasPlayingStarted(): Boolean {
        return playingStarted
    }
}