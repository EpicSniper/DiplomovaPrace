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
    private var scaleFactor = 1f

    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
        holder.addCallback(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        redrawAll()
        return true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        //redrawAll()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        redrawAll()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Stop drawing on the surface
    }

    private fun redrawAll() {
        var canvas = lockCanvas()
        canvas.save()
        var widthDifference = width - (width / scaleFactor)
        if (scrollX > 0f) {
            scrollX = 0f
        }

        var heightDifference = height - (height / scaleFactor)
        if (scrollY > 0f) {
            scrollY = 0f
        }

        if ((height / scaleFactor) - scrollY / scaleFactor > height) {
            scrollY = (scaleFactor - 1) * -height
        }

        var translateX = (scrollX / scaleFactor) + widthDifference / 2f
        var translateY = (scrollY / scaleFactor) + heightDifference / 2f

        canvas.drawColor(Color.GRAY)        // TODO: set background color
        canvas.scale(scaleFactor, scaleFactor, (width / 2f), (height / 2f))
        canvas.translate(translateX, translateY)
        drawPiano(canvas)
        canvas.drawRect(400f, 400f, 800f, 800f, paint)
        canvas.restore()
        unlockCanvas(canvas)
    }

    private fun lockCanvas(): Canvas {
        return holder.lockCanvas()
    }

    private fun unlockCanvas(canvas: Canvas) {
        holder.unlockCanvasAndPost(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        // Code to draw the grid lines


    }

    private fun drawPiano(canvas: Canvas) {
        // Draw piano keys
        val pianoKeyWidth = width / 16f
        val pianoKeyHeight = height / 127f // assuming 12 piano keys

        val pianoKeyColor = Color.BLUE
        paint.color = pianoKeyColor

        var counter = 0
        var widthDifference = width - width / scaleFactor
        val left = (-scrollX / scaleFactor)
        val right = pianoKeyWidth + left
        for (i in 0 until 127) {
            val top = i * pianoKeyHeight
            val bottom = top + pianoKeyHeight
            if (counter == 126) {
                paint.color = Color.BLUE
            }

            canvas.drawRect(left, top, right, bottom, paint)
            paint.color = Color.RED
            counter++
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
        scrollX -= distanceX
        scrollY -= distanceY

        var widthDifference = width - width / scaleFactor
        if (scrollX - widthDifference > 0f) {
            scrollX = 0f + widthDifference
        }

        var heightDifference = height - height / scaleFactor



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
        println("-" + detector.currentSpanX + "+" + detector.currentSpanY)*/
        scaleFactor *= detector.scaleFactor
        if (scaleFactor < 1f) {
            scaleFactor = 1f
        }

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        //println("OnScaleEnd")
    }
}