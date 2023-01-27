package cz.uhk.diplomovaprace.PianoRoll

import android.graphics.*

class Renderer(override val width: Int,
               override val height: Int,
               override var offsetX: Float,
               override var offsetY: Float,
               override var scaleX: Float,
               override var scaleY: Float, ) : RendererInterface {

    private val paint = Paint()

    override fun drawPiano(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        val pianoKeyWidth = width / 8f
        val pianoKeyHeight = height / 127f
        val pianoWhiteKeyColor = Color.WHITE
        val pianoBlackKeyColor = Color.BLACK

        for (i in 0 until 127) {
            val noteName = i % 12
            val top = i * pianoKeyHeight
            val left = 0f
            val bottom = top + pianoKeyHeight
            val right = pianoKeyWidth
            var rect = RectF(left, top, right, bottom)
            when(noteName){
                0, 2, 4, 5, 7, 9, 11 -> {
                    paint.color = pianoWhiteKeyColor
                }
                else -> {
                    paint.color = pianoBlackKeyColor
                }
            }

            canvas.drawRect(rect, paint)
        }
    }

    override fun drawNote(canvas: Canvas) {
        TODO("Not yet implemented")
    }

    override fun drawGrid(canvas: Canvas) {
        TODO("Not yet implemented")
    }

}