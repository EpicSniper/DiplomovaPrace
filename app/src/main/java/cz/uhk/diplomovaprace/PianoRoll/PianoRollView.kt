package cz.uhk.diplomovaprace.PianoRoll

import android.content.Context
import android.graphics.*
import android.graphics.Canvas.VertexMode
import android.util.AttributeSet
import android.view.*
import android.view.GestureDetector.OnGestureListener

class PianoRollView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback, OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private var paint = Paint()
    private var notes = ArrayList<Note>()
    // TODO: vsechno dat do ArrayList()<RectF> -> pohlidam si tim klikani, vim, kde co je

    private val gestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)

    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleFactorX = 1f
    private var scaleFactorY = 1f
    private var scaling = false
    private var scalingX = false
    private var scalingY = false
    private var startedSpanX = 0f
    private var startedSpanY = 0f
    private var widthDifference = 0f
    private var heightDifference = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var timelineHeight = height / 20f
    private var pianoKeyWidth = width / 16f
    private var pianoKeyHeight = height / 128f

    private var barTimeSignature = 4 / 4f
    private var beatLength = 480
    private var barLength = barTimeSignature * 4 * beatLength


    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
        paint.hinting = Paint.HINTING_OFF
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
        timelineHeight = height / 20f / scaleFactorY            // TODO: Aby uzivatel tuto promennou mohl menit
        pianoKeyWidth = width / 7f / scaleFactorX               // TODO: Aby uzivatel tuto promennou mohl menit
        pianoKeyHeight = (height - timelineHeight) / 128f

        barTimeSignature = 4 / 4f
        beatLength = 480
        barLength = barTimeSignature * 4 * beatLength

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
        timelineHeight = height / 12f / scaleFactorY
        pianoKeyWidth = width / 7f / scaleFactorX       // TODO: sirka klaves pres nastaveni
        pianoKeyHeight = (height - timelineHeight) / 128f

        canvas.drawColor(Color.GRAY)        // TODO: set background color

        // Zde se provadi transformace sceny
        canvas.translate(-scrollX, -scrollY)
        canvas.scale(scaleFactorX, scaleFactorY, centerX, centerY)

        drawGrid(canvas)
        drawNotes(canvas)
        drawTimelineAndPiano(canvas)
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
        if (scaleFactorX < 0.01f) {
            scaleFactorX = 0.01f
        }

        if (scaleFactorY < 1f) {
            scaleFactorY = 1f
        } else if (scaleFactorY > 10f) {
            scaleFactorY = 10f
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

    private fun drawTimelineAndPiano(canvas: Canvas)  {
        // draw timeline
        paint.color = Color.parseColor("#333333")   // TODO: barvy
        var left = scrollX + widthDifference / 2f
        var right = scrollX + width - (widthDifference / 2f)
        var top = scrollY + heightDifference / 2f
        var bottom = top + timelineHeight
        canvas.drawRect(left, top, right, bottom, paint)

        // draw time checkpoints (bars, ticks, etc.)
        // first visible bar
        var firstBar = left - (left % barLength) + pianoKeyWidth
        var sixteenthLengths = 0
        var actualTime = firstBar
        var topOfTheLine = top
        var upperColor = Color.parseColor("#ffffff")        // TODO: vsechny barvy
        var bottomColor = Color.parseColor("#222222")

        paint.textScaleX = scaleFactorY / scaleFactorX
        var barNumberCorrection = 1 - (pianoKeyWidth / barLength).toInt()
        do {
            var renderLines = true
            // vykreslit vsechny cary
            when (sixteenthLengths % 16) {
                0 -> {
                    topOfTheLine = top
                    upperColor = Color.parseColor("#ffffff")
                    bottomColor = Color.parseColor("#222222")
                    paint.textSize = timelineHeight / 4f
                    paint.color = upperColor
                    canvas.drawText(((actualTime / barLength).toInt() + barNumberCorrection).toString(), actualTime + 5, top + timelineHeight / 4f, paint)
                }

                1, 3, 5, 7, 9, 11, 13, 15 -> {
                    if (scaleFactorX > 0.32f) {
                        topOfTheLine = top + (timelineHeight / 16f * 12f )
                        upperColor = Color.parseColor("#bbbbbb")
                        bottomColor = Color.parseColor("#666666")
                    } else {
                        renderLines = false
                    }
                }

                2, 6, 10, 14 -> {
                    if (scaleFactorX > 0.16f) {
                        topOfTheLine = top + (timelineHeight / 16f * 11f )
                        upperColor = Color.parseColor("#cccccc")
                        bottomColor = Color.parseColor("#555555")
                    } else {
                        renderLines = false
                    }
                }

                4, 12 -> {
                    if (scaleFactorX > 0.08f) {
                        topOfTheLine = top + (timelineHeight / 16f * 10f )
                        upperColor = Color.parseColor("#dddddd")
                        bottomColor = Color.parseColor("#444444")
                    } else {
                        renderLines = false
                    }

                }

                8 -> {
                    if (scaleFactorX > 0.04f) {
                        topOfTheLine = top + (timelineHeight / 16f * 8f )
                        upperColor = Color.parseColor("#eeeeee")
                        bottomColor = Color.parseColor("#333333")
                    } else {
                        renderLines = false
                    }
                }
            }

            if (renderLines) {
                paint.color = upperColor
                canvas.drawLine(actualTime, topOfTheLine, actualTime, bottom, paint)
                paint.color = bottomColor
                canvas.drawLine(actualTime, bottom, actualTime, height.toFloat(), paint)
            }

            actualTime += beatLength / 4f
            sixteenthLengths++

        } while (actualTime < scrollX + width - (widthDifference / 2f))

        // draw piano
        drawPiano(canvas)

        // draw clear area
        paint.color = Color.parseColor("#333333")
        right = pianoKeyWidth + left
        canvas.drawRect(left, top, right, bottom, paint)
    }

    private fun drawGrid(canvas: Canvas) {
        val border = pianoKeyHeight / 20f

        val blackPianoKey = Color.parseColor("#444444")     // TODO: colors
        val whitePianoKey = Color.parseColor("#CCCCCC")
        val left = scrollX + widthDifference / 2f
        val right = left + width / scaleFactorX

        for (i in 0 until 128) {
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

    // TODO: Budou potreba tyto metody pro konvert?
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

    // TODO: Je potreba barva jako vstupni atribut?
    private fun drawNote(canvas: Canvas, note: Note) {
        val border = pianoKeyHeight / 20f
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

        for (i in 0 until 128) {
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
                0 -> {
                    paint.color = Color.GRAY
                    canvas.drawRect(left, bottom - border, right, bottom, paint)

                    val scaleNumber = (i / 12) - 2
                    paint.textSize = pianoKeyHeight
                    paint.color = Color.DKGRAY
                    canvas.drawText("C$scaleNumber", left + 2f, bottom - pianoKeyHeight * 0.15f, paint)
                }

                5 -> {
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
        /*if (detector.currentSpanX > 80f) {
            scalingX = true
        }

        if (detector.currentSpanY > 80f) {
            scalingY = true
        }*/



        if (scalingX) {
            scaleFactorX *= detector.scaleFactor
        }

        if (scalingY) {
            scaleFactorY *= detector.scaleFactor
        }

        /*scaleFactorX *= detector.scaleFactor
        scaleFactorY *= detector.scaleFactor*/

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scaling = true
        startedSpanX = detector.currentSpanX
        startedSpanY = detector.currentSpanY

        if (detector.currentSpanX > detector.currentSpanY) {
            scalingX = true
        } else {
            scalingY = true
        }
        //println("OnScaleEnd")
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        scaling = false
        scalingX = false
        scalingY = false
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