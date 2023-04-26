package cz.uhk.diplomovaprace.PianoRoll.Midi

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MidiFactory {
    fun main(context: Context) {
        // Create a new MIDI file with one track
        val numTracks = 1       // TODO: get numtracks
        val format = 1
        val division = 480
        val headerChunk = createHeaderChunk(numTracks, format, division)

        val timeSignatureUpper = 3
        val timeSignatuerLower = 8
        val trackChunk = createTrackChunk(createNoteEvents())

        // Write the MIDI file to disk
        val file = File(context.getExternalFilesDir(null), "example.mid")
        val outputStream = FileOutputStream(file)
        //val outputStream = context.openFileOutput("example1.mid", Context.MODE_PRIVATE)
        // outputStream = FileOutputStream(File("example.mid"))


        val arrayByte = ArrayList<Byte>()
        arrayByte.add(24)
        outputStream.write(getByteArray())


        //outputStream.write(getByteArray())

        outputStream.close()

        val directory = context.filesDir
        val midiFilePath = File(directory, "example.mid").absolutePath
    }
    fun createHeaderChunk(numTracks: Int, format: Int, division: Int): ByteArray {
        val bytes = ByteArray(14)
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            .put("MThd".toByteArray())
            .putInt(6) // Chunk size
            .putShort(format.toShort())
            .putShort(numTracks.toShort())
            .putShort(division.toShort())
        return bytes
    }

    fun createTrackChunk(events: List<ByteArray>): ByteArray {
        val trackBytes = ByteArrayOutputStream()
        for (event in events) {
            trackBytes.write(event)
        }
        val trackData = trackBytes.toByteArray()
        val bytes = ByteArray(trackData.size + 8)
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            .put("MTrk".toByteArray())
            .putInt(trackData.size)
            .put(trackData)
        return bytes
    }

    fun createNoteEvents(): List<ByteArray> {
        val events = mutableListOf<ByteArray>()
        events.add(createNoteOnEvent(0, 60, 127))
        events.add(createNoteOffEvent(100, 60, 0))
        return events
    }

    fun createNoteOnEvent(deltaTime: Int, note: Int, velocity: Int): ByteArray {
        val bytes = ByteArray(4)
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            .put(deltaTime.toVariableLengthQuantity())
            .put(0x90.toByte())
            .put(note.toByte())
            .put(velocity.toByte())
        return bytes
    }

    fun createNoteOffEvent(deltaTime: Int, note: Int, velocity: Int): ByteArray {
        val bytes = ByteArray(4)
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            .put(deltaTime.toVariableLengthQuantity())
            .put(0x80.toByte())
            .put(note.toByte())
            .put(velocity.toByte())
        return bytes
    }

    fun Int.toVariableLengthQuantity(): ByteArray {
        val bytes = mutableListOf<Byte>()
        var value = this
        do {
            var byteValue = value and 0x7F
            value = value shr 7
            if (value > 0) {
                byteValue = byteValue or 0x80
            }
            bytes.add(byteValue.toByte())
        } while (value > 0)
        return bytes.toByteArray()
    }

    fun getByteArray(): ByteArray {
        val byteArray = ByteArray(97)
        byteArray.set(0,77)
        byteArray.set(1,0x54)
        byteArray.set(2,0x68)
        byteArray.set(3,0x64)
        byteArray.set(4,0x00)
        byteArray.set(5,0x00)
        byteArray.set(6,0x00)
        byteArray.set(7,0x06)
        byteArray.set(8,0x00)
        byteArray.set(9,0x01)
        byteArray.set(10,0x00)
        byteArray.set(11,0x02)
        byteArray.set(12,0x00)
        byteArray.set(13, 0xC0.toByte())
        byteArray.set(14,0x4D)
        byteArray.set(15,0x54)
        byteArray.set(16,0x72)
        byteArray.set(17,0x6B)
        byteArray.set(18,0x00)
        byteArray.set(19,0x00)
        byteArray.set(20,0x00)
        byteArray.set(21,0x23)
        byteArray.set(22,0x00)
        byteArray.set(23, 0xFF.toByte())
        byteArray.set(24,0x58)
        byteArray.set(25,0x04)
        byteArray.set(26,0x04)
        byteArray.set(27,0x02)
        byteArray.set(28,0x18)
        byteArray.set(29,0x08)
        byteArray.set(30,0x00)
        byteArray.set(31, 0xFF.toByte())
        byteArray.set(32,0x51)
        byteArray.set(33,0x03)
        byteArray.set(34,0x07)
        byteArray.set(35, 0xA1.toByte())
        byteArray.set(36,0x20)
        byteArray.set(37,0x00)
        byteArray.set(38, 0xFF.toByte())
        byteArray.set(39,0x03)
        byteArray.set(40,0x0B)
        byteArray.set(41,0x54)
        byteArray.set(42,0x65)
        byteArray.set(43,0x6D)
        byteArray.set(44,0x70)
        byteArray.set(45,0x6F)
        byteArray.set(46,0x20)
        byteArray.set(47,0x54)
        byteArray.set(48,0x72)
        byteArray.set(49,0x61)
        byteArray.set(50,0x63)
        byteArray.set(51,0x6B)
        byteArray.set(52, 0xBC.toByte())
        byteArray.set(53,0x00)
        byteArray.set(54, 0xFF.toByte())
        byteArray.set(55,0x2F)
        byteArray.set(56,0x00)
        byteArray.set(57,0x4D)
        byteArray.set(58,0x54)
        byteArray.set(59,0x72)
        byteArray.set(60,0x6B)
        byteArray.set(61,0x00)
        byteArray.set(62,0x00)
        byteArray.set(63,0x00)
        byteArray.set(64,0x20)
        byteArray.set(65,0x00)
        byteArray.set(66, 0xFF.toByte())
        byteArray.set(67,0x03)
        byteArray.set(68,0x0E)
        byteArray.set(69,0x4E)
        byteArray.set(70,0x65)
        byteArray.set(71,0x77)
        byteArray.set(72,0x20)
        byteArray.set(73,0x49)
        byteArray.set(74,0x6E)
        byteArray.set(75,0x73)
        byteArray.set(76,0x74)
        byteArray.set(77,0x72)
        byteArray.set(78,0x75)
        byteArray.set(79,0x6D)
        byteArray.set(80,0x65)
        byteArray.set(81,0x6E)
        byteArray.set(82,0x74)
        byteArray.set(83,0x06)
        byteArray.set(84, 0x90.toByte())
        byteArray.set(85,0x32)
        byteArray.set(86,0x64)
        byteArray.set(87, 0x87.toByte())
        byteArray.set(88,0x53)
        byteArray.set(89, 0x80.toByte())
        byteArray.set(90,0x32)
        byteArray.set(91,0x00)
        byteArray.set(92, 0xB4.toByte())
        byteArray.set(93,0x27)
        byteArray.set(94, 0xFF.toByte())
        byteArray.set(95,0x2F)
        byteArray.set(96,0x00)

        return byteArray
    }

}