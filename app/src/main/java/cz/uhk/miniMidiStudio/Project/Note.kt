package cz.uhk.miniMidiStudio.Project

import android.graphics.RectF
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    var pitch: Byte,
    var start: Int,
    var duration: Int,
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
) {
    init {
        require(pitch in 0..127) { "Pitch must be in range 0-127" }
    }

    fun getRectF(): RectF {
        return RectF(left, top, right, bottom)
    }

    fun setRectF(rectF: RectF) {
        left = rectF.left
        top = rectF.top
        right = rectF.right
        bottom = rectF.bottom
    }
}