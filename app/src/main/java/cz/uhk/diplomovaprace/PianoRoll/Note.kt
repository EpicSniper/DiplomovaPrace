package cz.uhk.diplomovaprace.PianoRoll

data class Note(var pitch: Int, var start: Int, var duration: Int) {
    init {
        require(pitch in 0..127) { "Pitch must be in range 0-127" }
    }
}