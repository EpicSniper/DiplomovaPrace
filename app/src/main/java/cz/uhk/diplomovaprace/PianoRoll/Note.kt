package cz.uhk.diplomovaprace.PianoRoll

import android.graphics.RectF

data class Note(var pitch: Int, var start: Int, var duration: Int, var rectF: RectF, var streamId: Int?) {
    init {
        require(pitch in 0..127) { "Pitch must be in range 0-127" }
    }
}