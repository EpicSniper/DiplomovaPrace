package cz.uhk.diplomovaprace.PianoRoll.Midi

import cz.uhk.diplomovaprace.PianoRoll.Note


class Track {
    private var notes = ArrayList<Note>()

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