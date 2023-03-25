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
    private var selectedNotes = ArrayList<Note>()
    private var playingNotes = ArrayList<Note>()
    private var pianoKeys = ArrayList<RectF>()  // TODO: vyuzit tento ArrayList
    private var buttons = ArrayList<RectF>() // 0: play/stop button
    // TODO: vsechno dat do ArrayList()<RectF> -> pohlidam si tim klikani, vim, kde co je

    private var drawThread: DrawThread? = null
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
    private var pianoKeyHeight = (height - timelineHeight) / 128f

    private var barTimeSignature = 4 / 4f
    private var beatLength = 480
    private var barLength = barTimeSignature * 4 * beatLength
    private var tempo = 60f

    private var isPlaying = false
    private var lineOnTime = 0f
    private var movingTimeLine = false
    private var elapsedTime = System.currentTimeMillis()
    private var lastFrameTime = System.currentTimeMillis()
    private var currentTime = System.currentTimeMillis()
    private var soundJava = SoundJava(context)

    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
        paint.hinting = Paint.HINTING_OFF
        holder.addCallback(this)
        setWillNotDraw(false)
    }

    private inner class DrawThread() : Thread() {
        override fun run() {
            while (isPlaying) {
                invalidate()
                // sleep(50) // kontrolovat FPS
            }
        }

        fun stopDrawing() {
            isPlaying = false
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        if (!isPlaying) {
            redrawAll()
        }

        return true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

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
        timelineHeight = height / 20f / scaleFactorY            // TODO: Aby uzivatel tuto promennou mohl menit?
        pianoKeyWidth = width / 7f / scaleFactorX               // TODO: Aby uzivatel tuto promennou mohl menit?
        pianoKeyHeight = (height - timelineHeight) / 128f

        barTimeSignature = 4 / 4f
        beatLength = 480
        barLength = barTimeSignature * 4 * beatLength
        tempo = 60f

        isPlaying = false
        lineOnTime = 0f
        movingTimeLine = false
        elapsedTime = System.currentTimeMillis()
        lastFrameTime = System.currentTimeMillis()
        currentTime = System.currentTimeMillis()

        rectFArrayListInicialization()

        debugAddNotes()

        drawThread = DrawThread()
        drawThread?.start()

        soundJava = SoundJava(context)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Stop drawing on the surface
        drawThread?.stopDrawing()
        drawThread = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        redrawAll()
    }

    private fun redrawAll() {
        var canvas = lockCanvas()
        canvas.save()

        checkBorders()                                      // two times checkBorders, because of clipping out
        widthDifference = width - (width / scaleFactorX)
        heightDifference = height - (height / scaleFactorY)
        checkBorders()

        centerX = scrollX + width / 2f
        centerY = scrollY + height / 2f
        timelineHeight = height / 12f / scaleFactorY
        pianoKeyWidth = width / 7f / scaleFactorX       // TODO: sirka klaves pres nastaveni
        pianoKeyHeight = (height - timelineHeight) / 128f

        canvas.drawColor(Color.GRAY)        // TODO: set background color

        // Zde se provadi transformace sceny
        canvas.translate(-scrollX, -scrollY)
        canvas.scale(scaleFactorX, scaleFactorY, centerX, centerY)

        // rendering
        drawGrid(canvas)
        rescaleRectsOfNotes(notes)
        drawNotes(canvas)
        drawTimelineAndPiano(canvas)
        drawButtons(canvas)

        //drawDebugLines(canvas)

        // playing
        if (isPlaying) {
            playNotes(canvas)
        }


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

    private fun drawPlayline(canvas: Canvas) {
        if (isPlaying) {
            currentTime = System.currentTimeMillis()
            elapsedTime = currentTime - lastFrameTime
            lineOnTime += ((60f / tempo) * beatLength) * elapsedTime / 1000f
            lastFrameTime = currentTime
        }

        paint.color = Color.WHITE
        paint.strokeWidth = 10f / scaleFactorX
        canvas.drawLine(lineOnTime + pianoKeyWidth, 0f, lineOnTime + pianoKeyWidth, height.toFloat(), paint)
        paint.strokeWidth = 0f
    }

    private fun resetTime() {
        lastFrameTime = System.currentTimeMillis()
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


        // draw playLine
        drawPlayline(canvas)

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

    private fun rectFArrayListInicialization() {
        buttons.add(0, RectF(0f, 0f, 0f, 0f))   // index 0: play/stop button
        inicializePianoKeys()
    }

    private fun inicializePianoKeys() {
        val left = scrollX + widthDifference / 2f
        val right = pianoKeyWidth + left

        for (i in 0 until 128) {
            // Vykresleni vnejsi casti
            val bottom = height - (i * pianoKeyHeight)
            val top = bottom - pianoKeyHeight
            val rectF = RectF(left, top, right, bottom)
            pianoKeys.add(rectF)
        }
    }

    private fun drawButtons(canvas: Canvas) {
        // play/stop button
        paint.color = Color.RED             // TODO: color
        val top = scrollY + heightDifference / 2f
        val buttonBottom = top + (height - height / 30f) / scaleFactorY
        val buttonTop = top + (height - height / 10f) / scaleFactorY
        val playButtonHeight = (buttonBottom - buttonTop) * scaleFactorY /scaleFactorX
        val playButtonLeft = ((width - playButtonHeight) / 2f) + scrollX
        buttons[0] = RectF(playButtonLeft, buttonTop, playButtonLeft + playButtonHeight, buttonBottom)
        //canvas.drawRect(buttons[0], paint)
        val widthOfRectangle = playButtonHeight / 5f

        if (isPlaying) {
            // rendering pause (2 rectangles)
            canvas.drawRect(playButtonLeft + widthOfRectangle, buttonTop, playButtonLeft + widthOfRectangle * 2f, buttonBottom, paint)
            canvas.drawRect(playButtonLeft + widthOfRectangle * 3f, buttonTop, playButtonLeft + widthOfRectangle * 4f, buttonBottom, paint)
        } else {
            // draw triangle
            val triangleVerticies = floatArrayOf(
                playButtonLeft + widthOfRectangle, buttonTop,  // top vertex
                playButtonLeft + widthOfRectangle, buttonBottom,  // bottom vertex
                playButtonLeft + playButtonHeight, buttonTop + (buttonBottom - buttonTop) / 2f   // right vertex
            )

            val colors = intArrayOf(
                Color.RED, Color.RED, Color.RED, -0x1000000, -0x1000000, -0x1000000         // TODO: color
            )

            val vertexCount = triangleVerticies.size

            canvas.drawVertices(
                VertexMode.TRIANGLES, vertexCount, triangleVerticies,
                0,null,0,
                colors.map { it.toInt() }.toIntArray(),
                0, null,0, 0, paint
            )
        }

        // TODO: edit note button???
        // TODO: other buttons???
    }

    // TODO: Budou potreba tyto metody pro konvert?
    private fun pitchToHeightConverter(pitch: Int): Float {
        return height / 2f  // FIXME: placeholder
    }

    private fun heightToPitchConverter(height: Float): Int {
        return 60           // FIXME: placeholder
    }

    private fun pitchToNameConverter(pitch: Int): String {
        var noteName = "";
        when (pitch % 12) {
            0 -> noteName += "c"
            1 -> noteName += "cis"
            2 -> noteName += "d"
            3 -> noteName += "dis"
            4 -> noteName += "e"
            5 -> noteName += "f"
            6 -> noteName += "fis"
            7 -> noteName += "g"
            8 -> noteName += "gis"
            9 -> noteName += "a"
            10 -> noteName += "ais"
            11 -> noteName += "b"
        }

        var scaleNumber = (pitch / 12) - 2
        var scaleStringNumber = scaleNumber.toString().replace("-", "m")
        noteName += scaleStringNumber
        return noteName
    }

    private fun drawNotes(canvas: Canvas) {
        notes.forEach {
            drawNote(canvas, it)        // TODO: barva noty
        }
    }

    // TODO: Je potreba barva jako vstupni atribut?
    private fun drawNote(canvas: Canvas, note: Note) {
        val border = pianoKeyHeight / 20f

        // Namalovat okraje
        var noteRectF = note.rectF
        paint.color = Color.DKGRAY              // TODO: barvy
        canvas.drawRect(noteRectF, paint)

        paint.color = Color.BLUE
        // Namalovat vnitrek
        selectedNotes.forEach {
            if (it == note) {
                paint.color = Color.YELLOW
            }
        }

        canvas.drawRect(noteRectF.left + border, noteRectF.top + border, noteRectF.right - border, noteRectF.bottom - border, paint)
    }

    private fun drawPiano(canvas: Canvas) {
        // Draw piano keys
        val border = pianoKeyHeight / 20f

        val blackPianoKey = Color.BLACK
        val whitePianoKey = Color.WHITE

        val left = scrollX + widthDifference / 2f
        val right = pianoKeyWidth + left

        pianoKeys.forEachIndexed { i, it ->
            it.left = left
            it.right = right
            it.bottom = height - (i * pianoKeyHeight)
            it.top = it.bottom - pianoKeyHeight

            var key = i % 12
            when (key) {
                0, 2, 4, 5, 7, 9, 11 -> paint.color = whitePianoKey
                1, 3, 6, 8, 10 -> paint.color = blackPianoKey
            }

            canvas.drawRect(it, paint)
            when (key) {
                0 -> {
                    paint.color = Color.GRAY
                    canvas.drawRect(it.left, it.bottom - border, it.right, it.bottom, paint)

                    val scaleNumber = (i / 12) - 2
                    paint.textSize = pianoKeyHeight * 0.6f
                    paint.color = Color.DKGRAY
                    canvas.drawText("C$scaleNumber", it.left + 2f, it.bottom - pianoKeyHeight * 0.15f, paint)
                }

                5 -> {
                    paint.color = Color.GRAY
                    canvas.drawRect(it.left, it.bottom - border, it.right, it.bottom, paint)
                }

                4, 11 -> {
                    paint.color = Color.GRAY
                    canvas.drawRect(it.left, it.top, it.right, it.top + border, paint)
                }
            }
        }
    }

    private fun playNotes(canvas: Canvas) {
        notes.forEach {
            if (lineOnTime >= it.start) {

                if (playingNotes.contains(it)) {
                    if (lineOnTime > it.start + it.duration) {
                        // stop note
                        playingNotes.remove(it)
                        soundJava.stopSound(it.streamId)
                    }
                } else {
                    if (lineOnTime > it.start + it.duration) {

                    } else {
                        // play note
                        playingNotes.add(it)
                        it.streamId = soundJava.playSound(pitchToNameConverter(it.pitch))
                    }
                }
            }
        }
    }

    fun getNotes(): ArrayList<Note> {
        return this.notes
    }

    fun setNotes(notes: ArrayList<Note>) {
        this.notes = notes
        //redrawAll()
    }

    fun rescaleRectsOfNotes(notes: ArrayList<Note>) {
        notes.forEach{
            it.rectF = getRectFromNoteInfo(it.pitch, it.start, it.duration)
        }
    }

    fun getRectFromNoteInfo(pitch: Int, start: Int, duration: Int): RectF {
        var bottom = height - (pitch * pianoKeyHeight)
        var top = bottom - pianoKeyHeight
        var left = start + pianoKeyWidth       // Posunuji o sirku klaves
        var right = left + duration
        return RectF(left, top, right, bottom)
    }

    private fun stopAllSounds() {
        playingNotes.forEach {
            soundJava.stopSound(it.streamId)
        }

        playingNotes.clear()
    }

    private fun onSingleTapUpEvent(eventX: Float, eventY: Float) {
        if(buttons[0].contains(eventX, eventY)) {
            isPlaying = !isPlaying
            if (isPlaying) {
                drawThread = DrawThread()
                drawThread?.start()
                resetTime()
                stopAllSounds()
            } else {
                stopAllSounds()
                drawThread?.stopDrawing()
                drawThread = null
            }
        }
    }

    private fun onDownEvent(eventX: Float, eventY: Float) {
        var top = scrollY + heightDifference / 2f
        var bottom = top + timelineHeight
        movingTimeLine = false
        if (eventY > top && eventY <= bottom) {
            // taplo se na timeline -> presunout line
            movingTimeLine = true
            if (eventX - pianoKeyWidth > 0) {
                lineOnTime = eventX - pianoKeyWidth
            } else {
                lineOnTime = 0f
            }
        }
    }

    private fun onScrollingEvent(eventX1: Float, eventY1: Float, eventX2: Float, eventY2: Float) {
        if (movingTimeLine) {
            if (eventX2 - pianoKeyWidth > 0) {
                lineOnTime = eventX2 - pianoKeyWidth
            } else {
                lineOnTime = 0f
            }
        }
    }

    private fun convertEventX(eventX: Float): Float {
        return scrollX + ((width - width / scaleFactorX) / 2f) + (eventX / scaleFactorX)
    }

    private fun convertEventY(eventY: Float): Float {
        return scrollY + ((height - height / scaleFactorY) / 2f) + (eventY / scaleFactorY)
    }

    override fun onDown(event: MotionEvent): Boolean {
        /*println("------- ON DOWN -------")
        println("X: " + event.x + " |Y: " + event.y)*/
        val actualTapX = convertEventX(event.x)
        val actualTapY = convertEventY(event.y)
        onDownEvent(actualTapX, actualTapY)
        return true
    }

    override fun onShowPress(event: MotionEvent) {
        /*println("------- ON SHOW PRESS -------")
        println("X: " + event.x + " |Y: " + event.y)*/
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        /*println("------- ON SINGLE TAP -------")
        println("X: " + event.x + " |Y: " + event.y)*/
        val actualTapX = convertEventX(event.x)
        val actualTapY = convertEventY(event.y)

        onSingleTapUpEvent(actualTapX, actualTapY)
        return true
    }

    override fun onScroll(event1: MotionEvent, event2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        /*println("------- ON SCROLL -------")
        println("DOWN - X: " + event1.x + " |Y: " + event1.y)
        println("DOWN - X: " + event2.x + " |Y: " + event2.y)
        println("DISTANCE - X: " + distanceX + " |Y: " + distanceY)*/
        if (!scaling && !movingTimeLine) {
            scrollX += distanceX / scaleFactorX
            scrollY += distanceY / scaleFactorY
        }

        onScrollingEvent(convertEventX(event1.x), convertEventY(event1.y), convertEventX(event2.x), convertEventY(event2.y))
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

        if (scalingX && !movingTimeLine) {
            scaleFactorX *= detector.scaleFactor
        }

        if (scalingY && !movingTimeLine) {
            scaleFactorY *= detector.scaleFactor
        }

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scaling = true
        startedSpanX = detector.currentSpanX
        startedSpanY = detector.currentSpanY

        if (detector.currentSpanX * 0.8f > detector.currentSpanY) {
            scalingX = true
        } else if (detector.currentSpanY * 0.8f > detector.currentSpanX) {
            scalingY = true
        } else {
            scalingY = true
            scalingX = true
        }

        //println("OnScaleBegin")
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
    }

    private fun debugAddNotes() {
        var rectF = getRectFromNoteInfo(60, 0, 480)
        var note = Note(60, 0,480, rectF, null)
        notes.add(note)

        selectedNotes.add(note)

        rectF = getRectFromNoteInfo(64, 240,480)
        note = Note(64, 240,960, rectF, null)
        notes.add(note)

        rectF = getRectFromNoteInfo(64, 1440,960)
        note = Note(60, 1440,480, rectF, null)
        notes.add(note)

        rectF = getRectFromNoteInfo(64, 1440,960)
        note = Note(64, 1440,960, rectF, null)
        notes.add(note)
    }
}