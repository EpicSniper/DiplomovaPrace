package cz.uhk.miniMidiStudio.PianoRoll.Midi

data class NoteEvent(val pitch: Byte, var time: Int, val noteOn: Boolean) {

}