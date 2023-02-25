package cz.uhk.diplomovaprace.PianoRoll

import android.content.Context
import android.graphics.*
import android.graphics.Canvas.VertexMode
import android.util.AttributeSet
import android.view.*
import android.view.GestureDetector.OnGestureListener
import androidx.annotation.ColorInt
import kotlin.math.sqrt

class PianoRollView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback, OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private val paint = Paint()
    private var notes = ArrayList<Note>()

    private val gestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)

    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleFactorX = 1f
    private var scaleFactorY = 1f
    private var scaling = false
    private var widthDifference = 0f
    private var heightDifference = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var pianoKeyWidth = width / 16f
    private var pianoKeyHeight = height / 127f

    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
        holder.addCallback(this)
        holder.addCallback(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        redrawAll()

        return true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        redrawAll()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Inicialiazce promennych
        // Promenne pro prvotni zobrazeni
        scrollX = 0f
        scrollY = 0f
        scaleFactorX = 1f
        scaleFactorY = 1f

        // Ostatni promenne
        scaling = false
        widthDifference = width - (width / scaleFactorX)
        heightDifference = height - (height / scaleFactorY)
        centerX = scrollX + width / 2f
        centerY = scrollY + height / 2f
        pianoKeyWidth = width / 16f         // TODO: fixovana sirka
        pianoKeyHeight = height / 127f

        debugAddNotes()
        redrawAll()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Stop drawing on the surface
    }

    private fun redrawAll() {
        var canvas = lockCanvas()
        canvas.save()
        checkBorders()

        widthDifference = width - (width / scaleFactorX)
        heightDifference = height - (height / scaleFactorY)
        centerX = scrollX + width / 2f
        centerY = scrollY + height / 2f
        pianoKeyWidth = width / 7f / scaleFactorX       // TODO: sirka klaves pres nastaveni

        canvas.drawColor(Color.GRAY)        // TODO: set background color

        canvas.translate(-scrollX, -scrollY)
        canvas.scale(scaleFactorX, scaleFactorY, centerX, centerY)

        drawGrid(canvas)

        drawNotes(canvas)
        drawPiano(canvas)
        //drawDebugLines(canvas)

        canvas.restore()
        unlockCanvas(canvas)
    }

    private fun lockCanvas(): Canvas {
        return holder.lockCanvas()
    }

    private fun unlockCanvas(canvas: Canvas) {
        holder.unlockCanvasAndPost(canvas)
    }

    private fun checkBorders() {
        if (scaleFactorX < 1f) {
            scaleFactorX = 1f
        }

        if (scaleFactorY < 1f) {
            scaleFactorY = 1f
        }

        if (scrollX + widthDifference / 2f < 0f) {
            scrollX = -widthDifference / 2f
        }

        if (scrollY + heightDifference / 2f < 0f) {
            scrollY = -heightDifference / 2f
        }

        if (scrollY - heightDifference / 2f > 0f) {
            scrollY = heightDifference / 2f
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val border = pianoKeyHeight / 20f

        val blackPianoKey = Color.parseColor("#444444")     // TODO: colors
        val whitePianoKey = Color.parseColor("#CCCCCC")

        val left = scrollX
        val right = left + width

        for (i in 0 until 127) {
            // Vykresleni vnejsi casti
            val bottom = height - (i * pianoKeyHeight)
            val top = bottom - pianoKeyHeight

            var key = i % 12

            when (key) {
                0, 2, 4, 5, 7, 9, 11 -> paint.color = whitePianoKey
                1, 3, 6, 8, 10 -> paint.color = blackPianoKey
            }

            canvas.drawRect(left, top, right, bottom, paint)
            when (key) {
                0, 5 -> {
                    paint.color = Color.GRAY
                    canvas.drawRect(left, bottom - border, right, bottom, paint)
                }

                4, 11 -> {
                    paint.color = Color.GRAY
                    canvas.drawRect(left, top, right, top + border, paint)
                }
            }
        }
    }

    // Budou potreba tyto metody pro konvert?
    private fun pitchToHeightConverter(pitch: Int): Float {
        return height / 2f  // FIXME: placeholder
    }

    private fun heightToPitchConverter(height: Float): Int {
        return 60           // FIXME: placeholder
    }

    private fun drawNotes(canvas: Canvas) {
        notes.forEach {
            drawNote(canvas, it)        // TODO: barva noty
        }
    }

    // Je potreba barva jako vstupni atribut?
    private fun drawNote(canvas: Canvas, note: Note) {
        val border = pianoKeyHeight / 10f
        val bottom = height - (note.pitch * pianoKeyHeight)
        val top = bottom - pianoKeyHeight
        val left = note.start + pianoKeyWidth       // Posunuji o sirku klaves
        val right = left + note.duration

        // namalovat okraje
        var noteRectF = RectF(left.toFloat(), top, right.toFloat(), bottom)
        paint.color = Color.DKGRAY              // TODO: barvy
        canvas.drawRect(noteRectF, paint)

        // namalovat vnitrek
        if (note.selected) {
            paint.color = Color.BLUE            // TODO: barvy
        } else {
            paint.color = Color.YELLOW
        }

        noteRectF.set(noteRectF.left + border, noteRectF.top + border, noteRectF.right - border, noteRectF.bottom - border)
        canvas.drawRect(noteRectF, paint)
    }

    private fun drawPiano(canvas: Canvas) {
        // Draw piano keys
        val border = pianoKeyHeight / 20f

        val blackPianoKey = Color.BLACK
        val whitePianoKey = Color.WHITE

        val left = scrollX + widthDifference / 2f
        val right = pianoKeyWidth + left

        for (i in 0 until 127) {
            // Vykresleni vnejsi casti
            val bottom = height - (i * pianoKeyHeight)
            val top = bottom - pianoKeyHeight

            var key = i % 12

            when (key) {
                0, 2, 4, 5, 7, 9, 11 -> paint.color = whitePianoKey
                1, 3, 6, 8, 10 -> paint.color = blackPianoKey
            }

            canvas.drawRect(left, top, right, bottom, paint)
            when (key) {
                0, 5 -> {
                    paint.color = Color.GRAY
                    canvas.drawRect(left, bottom - border, right, bottom, paint)
                }

                4, 11 -> {
                    paint.color = Color.GRAY
                    canvas.drawRect(left, top, right, top + border, paint)
                }
            }
        }
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
        var actualTapX = scrollX + ((width - width / scaleFactorX) / 2f) + (event.x / scaleFactorX)
        var actualTapY = scrollY + ((height - height / scaleFactorY) / 2f) + (event.x / scaleFactorY)

        return true
    }

    override fun onScroll(event1: MotionEvent, event2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        /*println("------- ON SCROLL -------")
        println("DOWN - X: " + event1.x + " |Y: " + event1.y)
        println("DOWN - X: " + event2.x + " |Y: " + event2.y)
        println("DISTANCE - X: " + distanceX + " |Y: " + distanceY)*/
        if (!scaling) {
            scrollX += distanceX / scaleFactorX
            scrollY += distanceY / scaleFactorY
        }

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
        scaleFactorX *= detector.scaleFactor
        scaleFactorY *= detector.scaleFactor

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scaling = true
        //println("OnScaleEnd")
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        scaling = false
        //println("OnScaleEnd")
    }

    private fun drawDebugLines(canvas: Canvas) {
        for (i in 0 until 20) {
            if (i % 2 == 0) {
                paint.color = Color.RED
            } else {
                paint.color = Color.GREEN
            }

            canvas.drawRect(i * 100f, 0f, (i * 100f) + 100f, 20f, paint)
        }

        for (i in 0 until 20) {
            if (i % 2 == 0) {
                paint.color = Color.RED
            } else {
                paint.color = Color.GREEN
            }

            canvas.drawRect(0f, i * 100f, 20f, (i * 100f) + 100f, paint)
        }

        canvas.drawRect(400f, 400f, 800f, 800f, paint)

        paint.color = Color.BLUE
        canvas.drawRect(centerX - 50f, centerY - 50f, centerX + 50f, centerY + 50f, paint)

        // Random vertex
        paint.color = Color.RED
        val vertices = floatArrayOf(
            100f, 100f,  // first vertex
            200f, 100f,  // second vertex
            150f, 200f   // third vertex
        )

        val colors = intArrayOf(
            Color.BLUE, Color.BLUE, Color.BLUE, -0x1000000, -0x1000000, -0x1000000
        )

        val vertexCount = vertices.size

        canvas.drawVertices(
            VertexMode.TRIANGLES, vertexCount, vertices,
            0,null,0,
            colors.map { it.toInt() }.toIntArray(),
            0, null,0, 0, paint
        )

        /*canvas.drawVertices(
            VertexMode.TRIANGLES,
            verts.size, verts.toFloatArray(), 0,
            null, 0, colors.map { it.toInt() }.toIntArray(), 0,
            null, 0, 0,
            paint
        )
        */
    }

    private fun debugAddNotes() {
        var note = Note(60, 0,480, false)
        notes.add(note)
        note = Note(64, 240,480, true)
        notes.add(note)
    }
}