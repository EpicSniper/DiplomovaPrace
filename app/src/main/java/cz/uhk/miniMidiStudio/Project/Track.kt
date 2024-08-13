package cz.uhk.miniMidiStudio.Project

import kotlinx.serialization.Serializable

@Serializable
class Track(
    private var notes: ArrayList<Note> = ArrayList(),
    private var name: String = "New track",
    private var audioFile: String? = null,
    private var getRecordingsStart: Int? = null
) {
    fun getRecordingsStart(): Int? {
        if (getRecordingsStart == null) {
            getRecordingsStart = Int.MAX_VALUE
        }

        return getRecordingsStart
    }

    fun setRecordingsStart(newRecordingsStart: Int) {
        getRecordingsStart = newRecordingsStart
    }

    fun getEnd(): Int {
        return notes.maxOfOrNull { it.start + it.duration } ?: Int.MIN_VALUE
    }

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
        return notes.minOfOrNull { it.start + it.duration } ?: Int.MIN_VALUE
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

    fun addNotes(newNotes: ArrayList<Note>) {
        notes.addAll(newNotes)
    }

    fun removeNote(note: Note) {
        notes.remove(note)
    }
}