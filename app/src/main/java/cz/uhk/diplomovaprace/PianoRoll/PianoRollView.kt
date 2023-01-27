package cz.uhk.diplomovaprace.PianoRoll

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import android.view.GestureDetector.OnGestureListener

class PianoRollView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback, OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private val paint = Paint()
    private var notes = ArrayList<Note>()

    private val gestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)

    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleOfX = 0f
    private var scaleOfY = 0f
    private var scaleFactor = 1f

    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
        holder.addCallback(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val canvas = lockCanvas()
        canvas.drawRect(200f, 200f, 500f, 500f, paint)
        unlockCanvas(canvas)
        //redrawAll()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Stop drawing on the surface
    }

    private fun redrawAll() {
        val canvas = lockCanvas()
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor, scaleOfX, scaleOfY)
        canvas.translate(scrollX, scrollY)
        canvas.drawColor(Color.GRAY)
        canvas.drawRect(200f, 200f, 500f, 500f, paint)
        canvas.restore()
        unlockCanvas(canvas)
    }

    private fun lockCanvas(): Canvas {
        val canvas = holder.lockCanvas()
        return canvas
    }

    private fun unlockCanvas(canvas: Canvas) {
        holder.unlockCanvasAndPost(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        // Code to draw the grid lines

        // Draw piano keys
        val pianoKeyWidth = width / 8f  // assuming 1/6th of the view is dedicated to piano keys
        val pianoKeyHeight = height / 127f // assuming 12 piano keys
        val pianoKeyColor = Color.BLUE
        paint.color = pianoKeyColor
        for (i in 0 until 127) {
            val top = i * pianoKeyHeight
            val left = 0f
            val bottom = top + pianoKeyHeight
            val right = pianoKeyWidth
            var rect = Rect(400, 400, 900, 900);
            canvas.drawRect(left, top, right, bottom, paint)
            paint.color = Color.RED
            canvas.drawRect(rect, paint)
        }
    }

    private fun drawNotes(canvas: Canvas) {
        // Code to draw the notes
    }

    fun getNotes(): ArrayList<Note> {
        return this.notes
    }

    fun setNotes(notes: ArrayList<Note>) {
        this.notes = notes
        redrawAll()
    }

    override fun onDown(event: MotionEvent): Boolean {
        /*println("------- ON DOWN -------")
        println("X: " + event.x + " |Y: " + event.y)*/
        return true
    }

    override fun onShowPress(event: MotionEvent) {
        /*println("------- ON SHOW PRESS -------")
        println("X: " + event.x + " |Y: " + event.y)*/
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        /*println("------- ON SINGLE TAP -------")
        println("X: " + event.x + " |Y: " + event.y)*/
        return true
    }

    override fun onScroll(event1: MotionEvent, event2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        /*println("------- ON SCROLL -------")
        println("DOWN - X: " + event1.x + " |Y: " + event1.y)
        println("DOWN - X: " + event2.x + " |Y: " + event2.y)
        println("DISTANCE - X: " + distanceX + " |Y: " + distanceY)*/
        scrollX -= distanceX / scaleFactor
        scrollY -= distanceY / scaleFactor
        redrawAll()

        return true
    }

    override fun onLongPress(event: MotionEvent) {
        /*println("------- ON LONG PRESS -------")
        println("X: " + event.x + " |Y: " + event.y)*/
    }

    override fun onFling(eventDown: MotionEvent, eventUp: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        /*println("------- ON FLING -------")
        println("DOWN - X: " + eventDown.x + " |Y: " + eventDown.y)
        println("UP - X: " + eventUp.x + " |Y: " + eventUp.y)
        println("VELOCITY - X: " + velocityX + " |Y: " + velocityY)*/
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        /*println("OnScale")
        println("----------"+detector.scaleFactor)
        println("-" + detector.focusX + "+" + detector.focusY)*/
        scaleOfX = detector.focusX
        scaleOfY = detector.focusY
        scaleFactor *= detector.scaleFactor
        redrawAll()
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        println("OnScaleBegin")
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        println("OnScaleEnd")
    }
}