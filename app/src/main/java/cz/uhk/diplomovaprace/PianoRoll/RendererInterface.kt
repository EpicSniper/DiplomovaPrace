package cz.uhk.diplomovaprace.PianoRoll

import android.graphics.Canvas

interface RendererInterface {
    val width: Int
    val height: Int
    var offsetX: Float
    var offsetY: Float
    var scaleX: Float
    var scaleY: Float
    fun drawPiano(canvas: Canvas)
    fun drawNote(canvas: Canvas)
    fun drawGrid(canvas: Canvas)
}