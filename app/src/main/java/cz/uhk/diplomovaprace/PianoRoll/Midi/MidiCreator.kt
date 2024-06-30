package cz.uhk.diplomovaprace.PianoRoll.Midi

import android.content.Context
import cz.uhk.diplomovaprace.PianoRoll.Note
import cz.uhk.diplomovaprace.Project.Track
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow

class MidiCreator {

    private var tracks = ArrayList<Track>()

    constructor() {
        tracks = ArrayList<Track>()
    }

    // Vytvořit data pro midi
    public fun addTrack(track: Track) {
        tracks.add(track)
    }

    private fun createTracks(): ArrayList<Byte>  {
        var tracksArray = ArrayList<Byte>()
        tracks.forEach {
            tracksArray.addAll(createTrack(it))
        }

        return tracksArray
    }

    private fun createTrack(track: Track): ArrayList<Byte> {
        var noteEvents = ArrayList<NoteEvent>()
        track.getNotes().forEach {
            noteEvents.add(NoteEvent(it.pitch, it.start, true))
            noteEvents.add(NoteEvent(it.pitch, it.start + it.duration, false))
        }

        noteEvents.sortBy { it.time }
        var correctedNoteEvents = ArrayList<NoteEvent>()
        noteEvents.forEachIndexed{i, it ->
            if (i != 0) {
                var noteEvent = NoteEvent(it.pitch, it.time, it.noteOn)
                noteEvent.time -= noteEvents[i-1].time
                correctedNoteEvents.add(noteEvent)
            } else {
                correctedNoteEvents.add(it)
            }
        }


        var pomTrackArray = ArrayList<Byte>()
        pomTrackArray.addAll(createTrackNameEvent("Inst"))
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

        var trackArray = ArrayList<Byte>()
        "MTrk".toByteArray().forEach {
            trackArray.add(it)
        }

        trackArray.addAll(byteArrayFromNumber(pomTrackArray.size, true, true))

        trackArray.addAll(pomTrackArray)
        return trackArray
    }

    // TODO: napsat
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

        var timeArray = ArrayList<Byte>()
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


    public fun createMidiData(context: Context, upperTimeSignature: Int, lowerTimeSignatuer: Int, bpm: Int): ArrayList<Byte> {
        var finalMidi = ArrayList<Byte>()
        finalMidi.addAll(createHeader(tracks.size + 1))
        finalMidi.addAll(createInfoTrack(upperTimeSignature, lowerTimeSignatuer, bpm))
        finalMidi.addAll(createTracks())

        val file = File(context.getExternalFilesDir(null), "example.mid")
        val outputStream = FileOutputStream(file)


        var byteArray = ByteArray(finalMidi.size)
        finalMidi.forEachIndexed { i, it ->
            byteArray.set(i, it)
        }
        outputStream.write(byteArray)


        //outputStream.write(getByteArray())

        outputStream.close()

        val directory = context.filesDir
        val midiFilePath = File(directory, "example.mid").absolutePath
        return finalMidi
    }

    // Funkce na vytvoření headeru
    private fun createHeader(numberOfTracks: Int): ArrayList<Byte> {
        val chunkInicializationArray = ArrayList<Byte>()
        "MThd".toByteArray().forEach {
            chunkInicializationArray.add(it)
        }

        val byteInfoTrackLength = byteArrayFromNumber(6, true, true)
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

        var wholeArray = ArrayList<Byte>()
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

    // Funkce na vytvoření tracku z pole not
    private fun createInstrumentTrack(notes: ArrayList<Note>) {

    }

    // integer = 270 -> fourBytes = [14, 1]
    public fun byteArrayFromNumber(integer: Int, writeAllBytes: Boolean, reverse: Boolean): ArrayList<Byte> {
        var byteArray = ArrayList<Byte>()
        var pomInteger = integer
        var pomValue = 0
        for (i in 0..3) {
            var value = (integer shr (i*8)).toByte()
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