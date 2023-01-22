package cz.uhk.diplomovaprace.PianoRoll

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceView

class PianoRollView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs) {

    private val paint = Paint()
    private var notes: List<Note> = emptyList()

    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
        drawNotes(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        // Code to draw the grid lines
    }

    private fun drawNotes(canvas: Canvas) {
        // Code to draw the notes
    }

    fun setData(notes: List<Note>) {
        this.notes = notes
        invalidate()
    }

    data class Note(val pitch: Int, val start: Float, val duration: Float)
}