package cz.uhk.miniMidiStudio.PianoRoll.Midi

import android.content.Context
import android.content.Intent
import android.os.storage.StorageManager
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import cz.uhk.miniMidiStudio.PianoRoll.Note
import cz.uhk.miniMidiStudio.Project.Project
import cz.uhk.miniMidiStudio.Project.Track
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow

class MidiCreator() {
    private var project = Project()

    public fun setProject(project: Project) {
        this.project = project
    }

    private fun createTracks(): ArrayList<Byte>  {
        val tracksArray = ArrayList<Byte>()
        project.getTracks().forEach {
            tracksArray.addAll(createTrack(it))
        }

        return tracksArray
    }

    private fun createTrack(track: Track): ArrayList<Byte> {
        val noteEvents = ArrayList<NoteEvent>()
        track.getNotes().forEach {
            noteEvents.add(NoteEvent(it.pitch, it.start, true))
            noteEvents.add(NoteEvent(it.pitch, it.start + it.duration, false))
        }

        noteEvents.sortBy { it.time }
        val correctedNoteEvents = ArrayList<NoteEvent>()
        noteEvents.forEachIndexed{i, it ->
            if (i != 0) {
                val noteEvent = NoteEvent(it.pitch, it.time, it.noteOn)
                noteEvent.time -= noteEvents[i-1].time
                correctedNoteEvents.add(noteEvent)
            } else {
                correctedNoteEvents.add(it)
            }
        }


        val pomTrackArray = ArrayList<Byte>()
        pomTrackArray.addAll(createTrackNameEvent(track.getName()))
        correctedNoteEvents.forEach {
            pomTrackArray.addAll(noteTimeEventToByte(it.time))
            if (it.noteOn) {
                pomTrackArray.add(0x90.toByte())
                pomTrackArray.add(it.pitch)
                pomTrackArray.add(100)
            } else {
                pomTrackArray.add(0x80.toByte())
                pomTrackArray.add(it.pitch)
                pomTrackArray.add(0)
            }
        }

        pomTrackArray.addAll(createEndOfTrackEvent())

        val trackArray = ArrayList<Byte>()
        "MTrk".toByteArray().forEach {
            trackArray.add(it)
        }

        trackArray.addAll(byteArrayFromNumber(pomTrackArray.size, true, true))

        trackArray.addAll(pomTrackArray)
        return trackArray
    }

    // Psát po 127 bytech
    // pokud je víc jak 127 bytu tak zapsat 0x80 + přebytek
    private fun noteTimeEventToByte(time: Int): ArrayList<Byte> {
        var timeArrayPom = ArrayList<Byte>()
        var remainTime = time
        do {
            val value = remainTime % 128
            timeArrayPom.add(value.toByte())
            remainTime /= 128
        } while (remainTime > 128)

        if (remainTime > 0) {
            timeArrayPom.add(remainTime.toByte())
        }

        val timeArray = ArrayList<Byte>()
        timeArrayPom.forEachIndexed{i, it ->
            if (i > 0) {
                timeArray.add((it + 128).toByte())
            } else {
                timeArray.add(it)
            }
        }

        timeArray.reverse()

        return timeArray
    }

    public fun shareMidiFile(context: Context) {
        val file = File(context.cacheDir, "${project.getUuid()}.mid")
        val contentUri = FileProvider.getUriForFile(context, "cz.uhk.miniMidiStudio.fileprovider", file)
        val path = FileProvider.getUriForFile(context, "cz.uhk.miniMidiStudio.fileprovider", createMidiData(context))

        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntent.type = "audio/midi"
        startActivity(context, Intent.createChooser(shareIntent, "Share to"), null)
    }


    public fun createMidiData(context: Context): File {
        val finalMidi = ArrayList<Byte>()
        finalMidi.addAll(createHeader(project.getTracks().size + 1))
        finalMidi.addAll(createInfoTrack(project.getTimeSignatureUpper(), project.getTimeSignatureLower(), project.getTempo()))
        finalMidi.addAll(createTracks())

        val directory = context.cacheDir

        val file = File(directory, "${project.getUuid()}.mid")
        val outputStream = FileOutputStream(file)


        val byteArray = ByteArray(finalMidi.size)
        finalMidi.forEachIndexed { i, it ->
            byteArray.set(i, it)
        }
        outputStream.write(byteArray)
        outputStream.close()
        return File(directory, "${project.getUuid()}.mid")
    }

    // Funkce na vytvoření headeru
    private fun createHeader(numberOfTracks: Int): ArrayList<Byte> {
        val chunkInicializationArray = ArrayList<Byte>()
        "MThd".toByteArray().forEach {
            chunkInicializationArray.add(it)
        }

        val byteInfoTrackLength = byteArrayFromNumber(project.getTracks().size, true, true)
        val headerSection = createHeaderEvent(numberOfTracks)
        var wholeArray = ArrayList<Byte>()
        wholeArray.addAll(chunkInicializationArray)
        wholeArray.addAll(byteInfoTrackLength)
        wholeArray.addAll(headerSection)

        return wholeArray
    }

    private fun createHeaderEvent(numberOfTracks: Int): ArrayList<Byte> {
        val headerEvent = ArrayList<Byte>()
        headerEvent.add(0)
        headerEvent.add(1)
        headerEvent.add(0)
        headerEvent.add(numberOfTracks.toByte())
        byteArrayFromNumber(480, false, true).forEach {
            headerEvent.add(it)
        }

        return headerEvent
    }

    // Funkce na vytvoření info tracku
    private fun createInfoTrack(upperTimeSignature: Int, lowerTimeSignatuer: Int, bpm: Int): ArrayList<Byte> {
        val chunkInicializationArray = ArrayList<Byte>()
        "MTrk".toByteArray().forEach {
            chunkInicializationArray.add(it)
        }

        val timeSignatureSection = createTimeSignatureEvent(upperTimeSignature, lowerTimeSignatuer)
        val tempoSection = createTempoEvent(bpm)
        val trackNameSection = createTrackNameEvent("Tempo track")
        val endOfTrackSection = createEndOfTrackEvent()

        val infoTrackLength = timeSignatureSection.size + tempoSection.size + trackNameSection.size + endOfTrackSection.size
        val byteInfoTrackLength = byteArrayFromNumber(infoTrackLength, true, true)

        val wholeArray = ArrayList<Byte>()
        wholeArray.addAll(chunkInicializationArray)
        wholeArray.addAll(byteInfoTrackLength)
        wholeArray.addAll(timeSignatureSection)
        wholeArray.addAll(tempoSection)
        wholeArray.addAll(trackNameSection)
        wholeArray.addAll(endOfTrackSection)

        return wholeArray
    }

    private fun createTimeSignatureEvent(upperTimeSignature: Int, lowerTimeSignatuer: Int): ArrayList<Byte> {
        val timeSignatureEvent = ArrayList<Byte>()
        timeSignatureEvent.add(0)
        timeSignatureEvent.add(255.toByte())
        timeSignatureEvent.add(88)
        timeSignatureEvent.add(4)

        // Data
        timeSignatureEvent.add(upperTimeSignature.toByte())
        var ByteLowerTimeSignatuer = 0
        when (lowerTimeSignatuer) {
            1 -> ByteLowerTimeSignatuer = 0
            2 -> ByteLowerTimeSignatuer = 1
            8 -> ByteLowerTimeSignatuer = 3
            else -> ByteLowerTimeSignatuer = 2
        }

        timeSignatureEvent.add(ByteLowerTimeSignatuer.toByte())
        timeSignatureEvent.add(24)
        timeSignatureEvent.add(8)
        return timeSignatureEvent
    }

    private fun createTempoEvent(bpm: Int): ArrayList<Byte> {
        val tempoEvent = ArrayList<Byte>()
        tempoEvent.add(0)
        tempoEvent.add(255.toByte())
        tempoEvent.add(81)
        tempoEvent.add(3)
        val microsecondsPerQuarterNote = (60f / bpm) * 1000000
        val byteArrayOfBpm = byteArrayFromNumber(microsecondsPerQuarterNote.toInt(), false, true)
        byteArrayOfBpm.forEach {
            tempoEvent.add(it)
        }

        return tempoEvent
    }

    private fun createTrackNameEvent(name: String): ArrayList<Byte> {
        val trackNameEvent = ArrayList<Byte>()
        trackNameEvent.add(0)
        trackNameEvent.add(255.toByte())
        trackNameEvent.add(3)
        trackNameEvent.add(name.length.toByte())
        name.toByteArray().forEach {
            trackNameEvent.add(it)
        }

        return trackNameEvent
    }

    private fun createEndOfTrackEvent(): ArrayList<Byte> {
        val endOfTrackEvent = ArrayList<Byte>()
        endOfTrackEvent.add(0)
        endOfTrackEvent.add(255.toByte())
        endOfTrackEvent.add(47)
        endOfTrackEvent.add(0)

        return endOfTrackEvent
    }

    // integer = 270 -> fourBytes = [14, 1]
    public fun byteArrayFromNumber(integer: Int, writeAllBytes: Boolean, reverse: Boolean): ArrayList<Byte> {
        val byteArray = ArrayList<Byte>()
        var pomInteger = integer
        var pomValue = 0
        for (i in 0..3) {
            val value = (integer shr (i*8)).toByte()
            if (value < 0) {

            }

            byteArray.add(value)
            pomValue = value.toInt()
            if (pomValue < 0) {
                pomValue = (value + 256)
            }

            pomInteger -= (pomValue * 256.0.pow(i.toDouble())).toInt()
            if (pomInteger == 0 && !writeAllBytes) {
                break
            }
        }

        if (reverse) {
            byteArray.reverse()
        }

        return byteArray
    }
}