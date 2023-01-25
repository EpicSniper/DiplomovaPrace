package cz.uhk.diplomovaprace.PianoRoll

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener

class PianoRollView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback, OnTouchListener {

    private val paint = Paint()
    private var notes: List<Note> = emptyList()
    private var gridNeedsRedraw = true
    private var mLastTouchY: Float = 0f
    private var mIsScrolling = false
    private var mVerticalOffset = 0f

    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
        gridNeedsRedraw = true
        holder.addCallback(this)
        setOnTouchListener(this)
    }


    override fun onTouch(view: View, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mLastTouchY = event.y
                mIsScrolling = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (mIsScrolling) {
                    // Calculate the change in Y position
                    val dy = event.y - mLastTouchY
                    mLastTouchY = event.y
                    // Scroll the view by the change in Y position
                    mVerticalOffset += dy
                    gridNeedsRedraw = true
                    redrawAll()
                    return true

                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsScrolling = false
                return true
            }
        }
        return false
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        gridNeedsRedraw = true
        redrawAll()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        setWillNotDraw(false)
        redrawAll()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Stop drawing on the surface
    }

    private fun redrawAll() {
        val canvas = holder.lockCanvas()
        canvas.drawColor(Color.GRAY)
        drawGrid(canvas)
        drawNotes(canvas)
        holder.unlockCanvasAndPost(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        if (gridNeedsRedraw == true){
            // Code to draw the grid lines

            // Draw piano keys
            val pianoKeyWidth = width / 6f  // assuming 1/6th of the view is dedicated to piano keys
            val pianoKeyHeight = height / 12f // assuming 12 piano keys
            val pianoKeyColor = Color.BLUE
            paint.color = pianoKeyColor
            for (i in 0 until 12) {
                val top = i * pianoKeyHeight + mVerticalOffset
                val left = 0f
                val bottom = left + pianoKeyHeight + mVerticalOffset
                val right = pianoKeyWidth
                canvas.drawRect(left, top, right, bottom, paint)
            }

            gridNeedsRedraw = false
        }
    }

    private fun drawNotes(canvas: Canvas) {
        // Code to draw the notes
    }

    fun setData(notes: List<Note>) {
        this.notes = notes
        redrawAll()
    }

    data class Note(val pitch: Int, val start: Float, val duration: Float)
}