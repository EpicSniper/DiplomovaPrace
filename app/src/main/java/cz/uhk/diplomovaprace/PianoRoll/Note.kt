package cz.uhk.diplomovaprace.PianoRoll

import android.graphics.RectF

data class Note(var pitch: Byte, var start: Int, var duration: Int, var rectF: RectF) {
    init {
        require(pitch in 0..127) { "Pitch must be in range 0-127" }
    }
}