package cz.uhk.diplomovaprace.Project

import cz.uhk.diplomovaprace.PianoRoll.Note
import kotlinx.serialization.Serializable

@Serializable
class Track(
    private var notes: ArrayList<Note> = ArrayList(),
    private var start: Int = 0,
    private var name: String = "",
    private var audioFile: String? = null
) {


    fun getAudioFile(): String? {
        return audioFile
    }

    fun setAudioFile(newAudioFile: String) {
        audioFile = newAudioFile
    }

    fun getName(): String {
        return name
    }

    fun setName(newName: String) {
        name = newName
    }

    fun getStart(): Int {
        return start
    }

    fun setStart(newStart: Int) {
        start = newStart
    }

    fun getNotes(): ArrayList<Note> {
        return notes
    }

    fun setNotes(newNotes: ArrayList<Note>) {
        notes = newNotes
    }

    fun addNote(note: Note) {
        notes.add(note)
    }

    fun removeNote(note: Note) {
        notes.remove(note)
    }
}