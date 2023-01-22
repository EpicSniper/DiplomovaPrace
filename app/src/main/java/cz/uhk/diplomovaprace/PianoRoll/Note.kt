package cz.uhk.diplomovaprace.PianoRoll

data class Note(val pitch: Int, val start: Int, val duration: Int) {
    init {
        require(pitch in 0..127) { "Pitch must be in range 0-127" }
    }
}