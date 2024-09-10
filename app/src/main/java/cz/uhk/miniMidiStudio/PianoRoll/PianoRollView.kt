package cz.uhk.miniMidiStudio.PianoRoll

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.AttributeSet
import android.view.*
import android.view.GestureDetector.OnGestureListener
import androidx.core.content.ContextCompat
import cz.uhk.miniMidiStudio.PianoRoll.Midi.MidiPlayer
import cz.uhk.miniMidiStudio.Project.Note
import cz.uhk.miniMidiStudio.Project.Project
import cz.uhk.miniMidiStudio.Project.Track
import cz.uhk.miniMidiStudio.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import cz.uhk.miniMidiStudio.Project.ProjectManager
import cz.uhk.miniMidiStudio.Settings.ProjectSettingsData
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.File
import java.util.UUID

class PianoRollView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback, OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private lateinit var fragment: PianoRollFragment

    private lateinit var recordedAudio: ByteArray

    private var pitchDetectionMethod = PitchDetectionMethod.AUTOCORRELATION
    private var paint = Paint()
    private var selectedNotes = ArrayList<Note>()
    private var playingNotes = ArrayList<Note>()
    private var pianoKeys = ArrayList<RectF>()

    private var movingNoteIndex = -1
    private var movingNote = false
    private var movingNoteStart = 0
    private var movingNotePitch = 0

    private var drawThread: DrawThread? = null
    private var recordThread: RecordThread? = null
    private val gestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)

    private var scrollX = 0f
    private var scrollY = 0f
    private var scaleFactorX = 0.2f
    private var scaleFactorY = 2.5f
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
    private var pianoKeyBorder = pianoKeyHeight / 20f

    private var upperTimeSignature = 4f
    private var lowerTimeSignature = 4f
    private var beatLength = 480
    private var barLength = (upperTimeSignature / lowerTimeSignature) * 4 * beatLength
    private var tempo = 60
    private var a4Height = 442f

    private var project = Project()
    private var activeTrack = Track()
    private var activeTrackIndex = -1

    public var isPlaying = false
    private var lineOnTime = 0f
    private var movingTimeLine = false
    private var elapsedTime = System.currentTimeMillis()
    private var lastFrameTime = System.currentTimeMillis()
    private var currentTime = System.currentTimeMillis()
    private var midiPlayer = MidiPlayer()
    private var playRecordings = false
    private var audioPlayers = ArrayList<AudioPlayer>()

    public var isRecording = false
    private var recordingLineTime = ArrayList<Float>()
    private var recordingLineAutocorrelation = ArrayList<Double>()
    private var noteHeights = ArrayList<Float>()
    private var recordedTrackUuid = ""

    public var isEditing = false
    public var isCreating = false
    private var creatingNoteStart = 0f
    private var creatingNotePitch = 0
    private var creatingNoteDuration = 0f
    private var creatingNote = false

    private lateinit var audioRecord: AudioRecord
    private lateinit var audioTrack: AudioTrack

    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.FILL
        paint.hinting = Paint.HINTING_OFF
        holder.addCallback(this)
        setWillNotDraw(false)
        clearAll()
    }

    private inner class DrawThread() : Thread() {
        override fun run() {
            while (isPlaying) {
                invalidate()
                // sleep(50) // kontrolovat FPS
            }
        }

        public fun stopDrawing() {
            isPlaying = false
            invalidate()
        }
    }

    private inner class RecordThread(
        private var mediaRecorder: MediaRecorder
    ) : Thread() {
        private var audioSource = MediaRecorder.AudioSource.MIC
        private var sampleRate = 44100
        private var channelConfig = AudioFormat.CHANNEL_IN_MONO
        private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
        private var bufferSize =
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        private lateinit var audioDataList: MutableList<Byte>

        @SuppressLint("MissingPermission")
        private var audioRecord =
            AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
        private lateinit var audioData: ShortArray

        @SuppressLint("RestrictedApi", "MissingPermission")
        override fun start() {
            super.start()

            audioSource = MediaRecorder.AudioSource.MIC
            sampleRate = 44100
            channelConfig = AudioFormat.CHANNEL_IN_MONO
            audioFormat = AudioFormat.ENCODING_PCM_16BIT
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            audioRecord =
                AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            audioData = ShortArray(bufferSize)

            audioRecord.startRecording()
        }

        override fun run() {
            val buffer = ByteArray(bufferSize)
            audioDataList = mutableListOf<Byte>()
            while (isRecording) {
                val samplesRead = audioRecord.read(buffer, 0, bufferSize)
                if (samplesRead > 0) {
                    audioDataList.addAll(buffer.take(samplesRead))
                }
                recordingLineTime.add(lineOnTime)
                recordingLineAutocorrelation.add(getPitch(ShortArray(bufferSize), sampleRate))
                invalidate()
            }
        }

        public fun stopRecording() {
            isRecording = false
            audioRecord.stop()
            audioRecord.release()
            mediaRecorder.stop()
            mediaRecorder.release()

            recordedAudio = audioDataList.toByteArray()
            convertRecordingToNotes()
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        if (!isPlaying || !isRecording) {
            redrawAll()
        }

        if (isCreating && creatingNote && event.action == MotionEvent.ACTION_UP) {
            creatingNote = false
            createNewNote()
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
        scaleFactorX = 0.2f
        scaleFactorY = 2.5f

        // Ostatni promenne
        scaling = false
        widthDifference = width - (width / scaleFactorX)
        heightDifference = height - (height / scaleFactorY)
        centerX = scrollX + width / 2f
        centerY = scrollY + height / 2f
        timelineHeight =
            height / 20f / scaleFactorY            // TODO: Aby uzivatel tuto promennou mohl menit?
        pianoKeyWidth =
            width / 7f / scaleFactorX               // TODO: Aby uzivatel tuto promennou mohl menit?
        pianoKeyHeight = (height - timelineHeight) / 128f
        pianoKeyBorder = pianoKeyHeight / 20f

        beatLength = 480
        barLength = (upperTimeSignature / lowerTimeSignature) * 4 * beatLength

        isPlaying = false
        lineOnTime = 0f
        movingTimeLine = false
        elapsedTime = System.currentTimeMillis()
        lastFrameTime = System.currentTimeMillis()
        currentTime = System.currentTimeMillis()

        isRecording = false
        isEditing = false

        rectFArrayListInicialization()

        midiPlayer = MidiPlayer()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Stop drawing on the surface
        drawThread?.stopDrawing()
        drawThread = null
        recordThread?.stopRecording()
        recordThread = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        redrawAll()
    }

    private fun redrawAll() {
        val canvas = lockCanvas()
        canvas.save()

        project.getTracks().forEach {
            /*println("------------------")
            println("Name: " + it.getName())
            println("Audio file start: " + it.getStart())
            println("Note start: " + it.getNotes().first().start)*/
        }

        updateButtonsInFragment()

        checkBorders()                                      // two times checkBorders, because of clipping out
        widthDifference = width - (width / scaleFactorX)
        heightDifference = height - (height / scaleFactorY)
        checkBorders()

        centerX = scrollX + width / 2f
        centerY = scrollY + height / 2f
        timelineHeight = height / 12f / scaleFactorY
        pianoKeyWidth = width / 7f / scaleFactorX       // TODO: sirka klaves pres nastaveni
        pianoKeyHeight = (height - timelineHeight) / 128f


        canvas.drawColor(Color.GRAY)

        // Zde se provadi transformace sceny
        canvas.translate(-scrollX, -scrollY)
        canvas.scale(scaleFactorX, scaleFactorY, centerX, centerY)

        // rendering
        drawGrid(canvas)
        rescaleRectsOfNotes(activeTrack.getNotes())
        drawNotes(canvas)
        drawTimelineAndPianoAndNotes(canvas)

        // playing
        if (isPlaying) {
            currentTime = System.currentTimeMillis()
            elapsedTime = currentTime - lastFrameTime
            lineOnTime += ((tempo / 60f) * beatLength) * elapsedTime / 1000f
            lastFrameTime = currentTime
            if (playRecordings) {
                // zjistit, v jake casti se nachazim
                // zapnout zvuk, kdyz je cas startu mensi nez cas kde jsem
                project.getTracks().forEachIndexed {index, it ->
                    val recordingStart = it.getRecordingsStart()!!
                    if (!audioPlayers[index].hasPlayingStarted() && recordingStart <= lineOnTime) {
                        val delay = lineOnTime - recordingStart
                        val delayInMs = ((delay * 1000 / beatLength).toInt() / tempo) * 60
                        audioPlayers[index].startPlaying(delayInMs)
                    }
                }
            } else {
                playNotes()
            }
        }

        setHertzToNotes(a4Height)
        canvas.restore()
        unlockCanvas(canvas)
    }

    public fun changeEditingMode() {
        isEditing = !isEditing
        if (!isEditing) {
            selectedNotes.clear()
        }

        invalidate()
    }

    private fun getPitch(audioData: ShortArray, sampleRate: Int): Double {
        val pitchDetection = PitchDetection()
        return when (pitchDetectionMethod) {
            PitchDetectionMethod.AUTOCORRELATION -> {
                pitchDetection.getAutocorrelationPitch(audioData, sampleRate)
            }

            PitchDetectionMethod.HPS -> {
                pitchDetection.getHPSPitch(audioData, sampleRate)
            }
        }
    }

    // a4Height - default 442Hz
    private fun setHertzToNotes(a4Height: Float) {
        var noteArray = ArrayList<Float>()
        for (i in 0 until 128) {
            noteArray.add(a4Height * 2f.pow((i - 69f) / 12f))
        }

        noteHeights = noteArray
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

        if (scaleFactorY < 2.5f) {
            scaleFactorY = 2.5f
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
        paint.color = Color.WHITE
        paint.strokeWidth = 10f / scaleFactorX
        canvas.drawLine(
            lineOnTime + pianoKeyWidth,
            0f,
            lineOnTime + pianoKeyWidth,
            height.toFloat(),
            paint
        )
        paint.strokeWidth = 0f
    }

    private fun convertRecordingToNotes() {
        var recordedNotes = ArrayList<Int>()
        val newNotes = ArrayList<Note>()
        val recordingStart = recordingLineTime[0].toInt()
        recordingLineAutocorrelation.forEach {
            recordedNotes.add(closestNote(it.toFloat()))
        }

        // snapping
        var lengthsInSnap = Array(128) { 0 }
        var activeSnap = 0
        val newRecordingLineTime = ArrayList<Float>()
        val newRecordedNotes = ArrayList<Int>()
        recordedNotes.forEachIndexed { i, note ->
            if (i != 0) {
                val noteSnap = snapNote(recordingLineTime[i].toInt())
                val noteLength = recordingLineTime[i].toInt() - recordingLineTime[i - 1].toInt()
                if (activeSnap == noteSnap) {
                    lengthsInSnap[note] += noteLength
                } else {
                    var remainingLength =
                        getSnapLength() - (recordingLineTime[i - 1] % getSnapLength()).toInt()
                    lengthsInSnap[note] += noteLength

                    // zjisti největší číslo v lengthsInSnap a zjisti jeho index
                    var max = 0
                    var maxIndex = 0
                    lengthsInSnap.forEachIndexed { index, value ->
                        if (value > max) {
                            max = value
                            maxIndex = index
                        }
                    }

                    newRecordingLineTime.add(activeSnap.toFloat())
                    newRecordedNotes.add(maxIndex)
                    lengthsInSnap = Array(128) { 0 }

                    do {
                        activeSnap += getSnapLength()
                        remainingLength = noteLength - remainingLength
                        if (remainingLength > getSnapLength()) {
                            newRecordingLineTime.add(activeSnap.toFloat())
                            newRecordedNotes.add(note)
                        }
                    } while (remainingLength > getSnapLength())
                    activeSnap = noteSnap
                    lengthsInSnap[note] += remainingLength
                }
            } else {
                activeSnap = snapNote(recordingLineTime[i].toInt())
            }
        }

        recordedNotes = newRecordedNotes
        recordingLineTime = newRecordingLineTime
        // snapping end
        var lastNote = 0
        var startOfTheLastNote = 0
        recordedNotes.forEachIndexed { i, note ->
            if (i != 0) {
                if (lastNote != note) {
                    val pitch = note.toByte()
                    val start = startOfTheLastNote
                    val duration = recordingLineTime[i].toInt() - startOfTheLastNote
                    val rectF = getRectFromNoteInfo(pitch, start, duration)
                    newNotes.add(
                        Note(
                            pitch,
                            start,
                            duration,
                            rectF.left,
                            rectF.top,
                            rectF.right,
                            rectF.bottom
                        )
                    )
                    lastNote = note
                    startOfTheLastNote = recordingLineTime[i].toInt()
                }
            } else {
                lastNote = note
                startOfTheLastNote = recordingLineTime[i].toInt()
            }
        }


        if (recordingLineTime.size > 0) {
            val pitch = lastNote.toByte()
            val start = startOfTheLastNote
            val duration = recordingLineTime.last().toInt() - startOfTheLastNote
            val rectF = getRectFromNoteInfo(pitch, start, duration)
            newNotes.add(
                Note(
                    pitch,
                    start,
                    duration,
                    rectF.left,
                    rectF.top,
                    rectF.right,
                    rectF.bottom
                )
            )
        }

        val newTrack = Track()
        newTrack.addNotes(newNotes)
        newTrack.setAudioFile(recordedTrackUuid)
        newTrack.setRecordingsStart(recordingStart)
        newTrack.setRecordedAudio(recordedAudio)

        // add new track to project and set it as active track
        project.addTrack(newTrack)
        activeTrack = newTrack
        activeTrackIndex = project.getTracks().size - 1

        recordingLineTime.clear()
        recordingLineAutocorrelation.clear()
    }

    // height in Hz
    private fun closestNote(height: Float): Int {
        var note = 0
        for (i in 1 until 128) {
            if (height < noteHeights[i]) {
                note = i - 1
                break
            }
        }

        if (height - noteHeights[note] > noteHeights[note + 1] - height) {
            note++
        }

        return note
    }

    private fun resetTime() {
        lastFrameTime = System.currentTimeMillis()
    }

    private fun drawTimelineAndPianoAndNotes(canvas: Canvas) {
        // draw timeline
        paint.color = ContextCompat.getColor(context, R.color.pianorollframe)
        val left = scrollX + widthDifference / 2f
        var right = scrollX + width - (widthDifference / 2f)
        val top = scrollY + heightDifference / 2f
        val bottom = top + timelineHeight
        canvas.drawRect(left, top, right, bottom, paint)

        // draw time checkpoints (bars, ticks, etc.)
        // first visible bar
        val firstBar = left - (left % barLength) + pianoKeyWidth
        var sixteenthLengths = 0
        var actualTime = firstBar
        var topOfTheLine = top
        var upperColor = ContextCompat.getColor(context, R.color.pinkie)
        var bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
        val upperLineThickness = 8f
        val bottomLineThickness = 4f

        paint.textScaleX = scaleFactorY / scaleFactorX
        val barNumberCorrection = 1 - (pianoKeyWidth / barLength).toInt()
        var textCorrectionX = actualTime
        var barNumberWidth = 0f
        var barNumber = "1"
        val sixteenthInBar = (barLength / beatLength * 4f).toInt()
        var converter = 0
        val modifier = if (sixteenthInBar < 16) sixteenthInBar else 16
        var linePositions = ArrayList<Float>()
        do {
            var renderLines = true

            barNumber = ((actualTime / barLength).toInt() + barNumberCorrection).toString()
            // vykreslit vsechny cary

            converter = sixteenthLengths % modifier
            when (converter % modifier) {
                0 -> {
                    if (sixteenthLengths % sixteenthInBar != 0) {
                        if (scaleFactorX > 0.08f) {
                            topOfTheLine = top + (timelineHeight / 16f * 10f)
                            upperColor = ContextCompat.getColor(context, R.color.text1)
                            bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                        } else {
                            renderLines = false
                        }
                    } else {
                        topOfTheLine = top + (timelineHeight / 16f * 8f)
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                        paint.textSize = timelineHeight / 4f
                        paint.color = upperColor
                        barNumberWidth = paint.measureText(barNumber)
                        if (barNumber == "1") {
                            textCorrectionX = barNumberWidth / 2f
                        } else {
                            textCorrectionX = -barNumberWidth / 2f
                        }
                        canvas.drawText(
                            barNumber,
                            actualTime + textCorrectionX,
                            top + timelineHeight / 4f,
                            paint
                        )
                    }
                }

                1, 3, 5, 7, 9, 11, 13, 15 -> {
                    if (scaleFactorX > 0.64f) {
                        topOfTheLine = top + (timelineHeight / 16f * 12f)
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    } else {
                        renderLines = false
                    }
                }

                2, 6, 10, 14 -> {
                    if (scaleFactorX > 0.32f) {
                        topOfTheLine = top + (timelineHeight / 16f * 11f)
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    } else {
                        renderLines = false
                    }
                }

                4, 12 -> {
                    if (scaleFactorX > 0.16f) {
                        topOfTheLine = top + (timelineHeight / 16f * 10f)
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    } else {
                        renderLines = false
                    }
                }

                8 -> {
                    if (scaleFactorX > 0.08f) {
                        topOfTheLine = top + (timelineHeight / 16f * 10f)
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    } else {
                        renderLines = false
                    }
                }
            }

            if (renderLines) {
                paint.color = upperColor
                paint.strokeWidth = upperLineThickness / scaleFactorX
                canvas.drawLine(actualTime, topOfTheLine, actualTime, bottom, paint)
                paint.strokeWidth = bottomLineThickness / scaleFactorX
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
        paint.color = ContextCompat.getColor(context, R.color.pianorollframe)
        right = pianoKeyWidth + left
        canvas.drawRect(left, top, right, bottom, paint)
    }

    private fun drawGrid(canvas: Canvas) {
        val blackPianoKey = ContextCompat.getColor(context, R.color.background2)
        val whitePianoKey = ContextCompat.getColor(context, R.color.background)
        val darkBorderColor = ContextCompat.getColor(context, R.color.pianorollframe)
        val lightBorderColor = ContextCompat.getColor(context, R.color.text1)
        val middleBorderColor = ContextCompat.getColor(context, R.color.pianorollline)
        val pinkieBorderColor = ContextCompat.getColor(context, R.color.pinkie)
        val left = scrollX + widthDifference / 2f
        val right = left + width / scaleFactorX

        var upperBorderColor = darkBorderColor
        var bottomBorderColor = lightBorderColor
        var keyColor = whitePianoKey

        for (i in 0 until 128) {
            // Vykresleni vnejsi casti
            val bottom = height - (i * pianoKeyHeight)
            val top = bottom - pianoKeyHeight

            var key = i % 12

            when (key) {
                0 -> {
                    keyColor = whitePianoKey
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = pinkieBorderColor
                }

                1 -> {
                    keyColor = blackPianoKey
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                2 -> {
                    keyColor = whitePianoKey
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = lightBorderColor
                }

                3 -> {
                    keyColor = blackPianoKey
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                4 -> {
                    keyColor = whitePianoKey
                    upperBorderColor = middleBorderColor
                    bottomBorderColor = lightBorderColor
                }

                5 -> {
                    keyColor = whitePianoKey
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = middleBorderColor
                }

                6 -> {
                    keyColor = blackPianoKey
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                7 -> {
                    keyColor = whitePianoKey
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = lightBorderColor
                }

                8 -> {
                    keyColor = blackPianoKey
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                9 -> {
                    keyColor = whitePianoKey
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = lightBorderColor
                }

                10 -> {
                    keyColor = blackPianoKey
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                11 -> {
                    keyColor = whitePianoKey
                    upperBorderColor = pinkieBorderColor
                    bottomBorderColor = lightBorderColor
                }
            }

            paint.color = keyColor
            canvas.drawRect(left, top, right, bottom, paint)
            paint.color = upperBorderColor
            canvas.drawRect(left, top, right, top + pianoKeyBorder, paint)
            paint.color = bottomBorderColor
            canvas.drawRect(left, bottom - pianoKeyBorder, right, bottom, paint)
        }
    }

    private fun rectFArrayListInicialization() {
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
            pianoKeys.add(i, rectF)
        }
    }

    private fun pitchToHeightConverter(pitch: Int): Float {
        return height / 2f  // FIXME: placeholder
    }

    private fun heightToPitchConverter(height: Float): Int {
        return pianoKeys.indexOfFirst {
            it.top < height && it.bottom > height
        }
    }

    private fun drawNotes(canvas: Canvas) {
        drawOtherTrackNotes(canvas)
        drawActiveNotes(canvas)
        if (creatingNote) {
            drawCreatingNote(canvas)
        }
    }

    private fun drawActiveNotes(canvas: Canvas) {
        activeTrack.getNotes().forEach { it ->
            var noteRectF = it.getRectF()

            paint.color = ContextCompat.getColor(context, R.color.note)
            // Namalovat vnitrek
            selectedNotes.forEach { note ->
                if (it == note) {
                    paint.color = ContextCompat.getColor(context, R.color.pinkie)
                }
            }

            noteRectF = RectF(
                noteRectF.left + pianoKeyBorder,
                noteRectF.top + pianoKeyBorder,
                noteRectF.right - pianoKeyBorder,
                noteRectF.bottom - pianoKeyBorder
            )

            drawNote(canvas, it, noteRectF, paint)
        }
    }

    private fun drawNote(canvas: Canvas, note: Note, noteRectF: RectF, paint: Paint) {
        val cornerRadiusX = (noteRectF.bottom - noteRectF.top) / 4f
        val cornerRadiusY = (noteRectF.bottom - noteRectF.top) * 2f / scaleFactorX
        canvas.drawRoundRect(noteRectF, cornerRadiusY, cornerRadiusX, paint)
    }

    private fun drawCreatingNote(canvas: Canvas) {
        val creatingNoteRectF = getRectFromNoteInfo(
            creatingNotePitch.toByte(),
            creatingNoteStart.toInt(),
            creatingNoteDuration.toInt()
        )
        val note = Note(
            creatingNotePitch.toByte(),
            creatingNoteStart.toInt(),
            creatingNoteDuration.toInt(),
            creatingNoteRectF.left,
            creatingNoteRectF.top,
            creatingNoteRectF.right,
            creatingNoteRectF.bottom
        )

        paint.color = ContextCompat.getColor(context, R.color.note)
        drawNote(canvas, note, creatingNoteRectF, paint)
    }

    private fun drawOtherTrackNotes(canvas: Canvas) {
        val otherNotes = project.getTracks().filter { it != activeTrack }.flatMap { it.getNotes() }
        otherNotes.forEach {
            var noteRectF = it.getRectF()

            paint.color = ContextCompat.getColor(context, R.color.darkKeyFont)

            noteRectF = RectF(
                noteRectF.left + pianoKeyBorder,
                noteRectF.top + pianoKeyBorder,
                noteRectF.right - pianoKeyBorder,
                noteRectF.bottom - pianoKeyBorder
            )

            drawNote(canvas, it, noteRectF, paint)
        }
    }

    private fun drawPiano(canvas: Canvas) {
        // Draw piano keys
        val blackPianoKey = ContextCompat.getColor(context, R.color.darkKey)
        val whitePianoKey = ContextCompat.getColor(context, R.color.lightKey)
        val blackKeyText = ContextCompat.getColor(context, R.color.darkKeyFont)
        val whiteKeyText = ContextCompat.getColor(context, R.color.lightKeyFont)
        val darkBorderColor = ContextCompat.getColor(context, R.color.pianorollframe)
        val lightBorderColor = ContextCompat.getColor(context, R.color.text1)
        val middleBorderColor = ContextCompat.getColor(context, R.color.pianorollline)
        val pinkieBorderColor = ContextCompat.getColor(context, R.color.pinkie)

        val left = scrollX + widthDifference / 2f
        val right = pianoKeyWidth + left
        val middleX = left + pianoKeyWidth / 2f

        var keyColor = whitePianoKey
        var textColor = whiteKeyText
        var keyText = "C"
        var textWidth = paint.measureText(keyText)
        var textSizeForWidth = getTextSizeForWidth(paint, keyText, pianoKeyWidth)
        var textSizeForHeight = getTextSizeForHeight(paint, pianoKeyHeight)
        var textWidthPadding = 0f
        var textHeightPadding = 0f
        var upperBorderColor = darkBorderColor
        var bottomBorderColor = lightBorderColor

        pianoKeys.forEachIndexed { i, it ->
            it.left = left
            it.right = right
            it.bottom = height - (i * pianoKeyHeight)
            it.top = it.bottom - pianoKeyHeight

            val key = i % 12
            val scaleNumber = (i / 12) - 2

            when (key) {
                0 -> {
                    keyColor = whitePianoKey
                    textColor = whiteKeyText
                    keyText = "C"
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = pinkieBorderColor
                }

                1 -> {
                    keyColor = blackPianoKey
                    textColor = blackKeyText
                    keyText = "C#"
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                2 -> {
                    keyColor = whitePianoKey
                    textColor = whiteKeyText
                    keyText = "D"
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = lightBorderColor
                }

                3 -> {
                    keyColor = blackPianoKey
                    textColor = blackKeyText
                    keyText = "D#"
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                4 -> {
                    keyColor = whitePianoKey
                    textColor = whiteKeyText
                    keyText = "E"
                    upperBorderColor = middleBorderColor
                    bottomBorderColor = lightBorderColor
                }

                5 -> {
                    keyColor = whitePianoKey
                    textColor = whiteKeyText
                    keyText = "F"
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = middleBorderColor
                }

                6 -> {
                    keyColor = blackPianoKey
                    textColor = blackKeyText
                    keyText = "F#"
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                7 -> {
                    keyColor = whitePianoKey
                    textColor = whiteKeyText
                    keyText = "G"
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = lightBorderColor
                }

                8 -> {
                    keyColor = blackPianoKey
                    textColor = blackKeyText
                    keyText = "G#"
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                9 -> {
                    keyColor = whitePianoKey
                    textColor = whiteKeyText
                    keyText = "A"
                    upperBorderColor = darkBorderColor
                    bottomBorderColor = lightBorderColor
                }

                10 -> {
                    keyColor = blackPianoKey
                    textColor = blackKeyText
                    keyText = "A#"
                    upperBorderColor = lightBorderColor
                    bottomBorderColor = darkBorderColor
                }

                11 -> {
                    keyColor = whitePianoKey
                    textColor = whiteKeyText
                    keyText = "B"
                    upperBorderColor = pinkieBorderColor
                    bottomBorderColor = lightBorderColor
                }
            }

            // draw piano key
            paint.color = keyColor
            val rect = RectF(it.left, it.top, it.right, it.bottom)
            val notRoundedRect = RectF(it.left, it.top, middleX, it.bottom)
            val cornerRadiusX = (it.bottom - it.top) / 5f
            val cornerRadiusY = (it.right - it.left) / 5f
            canvas.drawRoundRect(rect, cornerRadiusY, cornerRadiusX, paint)
            canvas.drawRect(notRoundedRect, paint)

            // draw key text
            paint.color = textColor
            keyText = "$keyText$scaleNumber"


            textSizeForWidth = getTextSizeForWidth(paint, keyText, pianoKeyWidth)
            textSizeForHeight = getTextSizeForHeight(paint, pianoKeyHeight)
            paint.textSize = textSizeForHeight


            if (textSizeForHeight > (it.bottom - it.top) * 0.8f) {
                paint.textSize = textSizeForHeight * 0.8f
            }

            if (textSizeForWidth > (it.left - it.right) * 0.8f && textSizeForWidth < textSizeForHeight) {
                paint.textSize = textSizeForWidth * 0.8f
            }

            textWidth = paint.measureText(keyText)
            textWidthPadding = (it.right - it.left - textWidth) / 2f

            textHeightPadding = ((it.bottom - it.top + (paint.descent() + paint.ascent())) / 2f)
            canvas.drawText(
                keyText,
                it.left + textWidthPadding,
                it.bottom - textHeightPadding,
                paint
            )

            // draw borders
            paint.color = bottomBorderColor
            canvas.drawRect(it.left, it.bottom - pianoKeyBorder, it.right, it.bottom, paint)
            paint.color = upperBorderColor
            canvas.drawRect(it.left, it.top, it.right, it.top + pianoKeyBorder, paint)
        }
    }

    private fun getTextSizeForHeight(paint: Paint, height: Float): Float {
        val testTextSize = 1f
        paint.textSize = testTextSize
        val textHeight = paint.descent() - paint.ascent()
        return testTextSize * height / textHeight
    }

    private fun getTextSizeForWidth(paint: Paint, str: String, width: Float): Float {
        val testTextSize = 1f
        paint.textSize = testTextSize
        return testTextSize * width / paint.measureText(str)
    }

    private fun playNotes() {
        project.getTracks().forEach {
            playNotesInTrack(it)
        }
    }

    private fun playNotesInTrack(track: Track) {
        track.getNotes().forEach {
            if (lineOnTime >= it.start) {
                if (playingNotes.contains(it)) {
                    if (lineOnTime > it.start + it.duration) {
                        // stop note
                        playingNotes.remove(it)
                        midiPlayer.stopNote(it.pitch)
                    }
                } else {
                    if (lineOnTime > it.start + it.duration) {

                    } else {
                        // play note
                        playingNotes.add(it)
                        midiPlayer.playNote(it.pitch)
                    }
                }
            }
        }
    }

    fun rescaleRectsOfNotes(notes: ArrayList<Note>) {
        notes.forEach {
            it.setRectF(getRectFromNoteInfo(it.pitch, it.start, it.duration))
        }
    }

    fun getRectFromNoteInfo(pitch: Byte, start: Int, duration: Int): RectF {
        val bottom = height - (pitch * pianoKeyHeight)
        val top = bottom - pianoKeyHeight
        val left = start + pianoKeyWidth       // Posunuji o sirku klaves
        val right = left + duration
        return RectF(left, top, right, bottom)
    }

    fun pushPlayButton() {
        if (!isPlaying && !isRecording) {
            isPlaying = true
            drawThread = DrawThread()
            drawThread?.start()
            resetTime()
            if (playRecordings) {
                initAudioPlayers()
            } else {
                midiPlayer.onMidiStart()
            }
        }
    }

    private fun initAudioPlayers() {
        audioPlayers.clear()
        project.getTracks().forEach {
            audioPlayers.add(AudioPlayer(it))
        }
    }

    @SuppressLint("MissingPermission")
    fun pushRecordButton() {
        if (!isRecording && !isPlaying) {
            isRecording = true
            recordedTrackUuid = UUID.randomUUID().toString().replace("-", "")
            val outputFileName = "$recordedTrackUuid.mp3"
            val file = File(context.filesDir, outputFileName)

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()

            recordThread = RecordThread(mediaRecorder)
            recordThread?.start()





            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioRecord =
                AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()


            isPlaying = true
            drawThread = DrawThread()
            drawThread?.start()
            resetTime()
            if (playRecordings) {
                initAudioPlayers()
            } else {
                midiPlayer.onMidiStart()
            }
        }
    }

    fun startRecording() : ByteArray {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val audioDataList = mutableListOf<Byte>()
        val buffer = ByteArray(bufferSize)

        isRecording = true
        audioRecord.startRecording()

        val recordingThread = Thread {
            while (isRecording) {
                val readBytes = audioRecord.read(buffer, 0, buffer.size)
                if (readBytes > 0) {
                    audioDataList.addAll(buffer.take(readBytes))
                }
            }
            audioRecord.stop()
        }
        recordingThread.start()
        recordingThread.join()

        return audioDataList.toByteArray()
    }


    fun playAudio(audioData: ByteArray) {
        audioTrack.play()

        val playThread = Thread {
            audioTrack.write(audioData, 0, audioData.size)
            audioTrack.stop()
            audioTrack.release()
        }

        playThread.start()
    }

    fun pushStopButton() {
        if (isPlaying) {
            isPlaying = false
            drawThread?.stopDrawing()
            drawThread = null
            playingNotes.clear()
            midiPlayer.stopAllNotes()
            audioPlayers.forEach {
                it.stopPlaying()
            }
        }

        if (isRecording) {
            isRecording = false
            recordThread?.stopRecording()
            recordThread = null
        }
    }

    private fun onSingleTapUpEvent(eventX: Float, eventY: Float) {
        if (isEditing) {
            activeTrack.getNotes().forEach {
                if (it.getRectF().contains(eventX, eventY)) {
                    if (selectedNotes.contains(it)) {
                        selectedNotes.remove(it)
                        if (selectedNotes.isEmpty()) {
                            changeEditingMode()
                        }
                    } else {
                        selectedNotes.add(it)
                    }

                    GlobalScope.launch {
                        midiPlayer.playNote(it.pitch)
                        delay(250)
                        midiPlayer.stopNote(it.pitch)
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun onDownEvent(eventX: Float, eventY: Float) {
        if (isEditing) {
            movingNote = false
            movingNoteIndex = -1
        }
        // timeline
        var top = scrollY + heightDifference / 2f
        var bottom = top + timelineHeight
        movingTimeLine = false
        if (convertEventY(eventY) > top && convertEventY(eventY) <= bottom) {
            // taplo se na timeline -> presunout line
            pushStopButton()
            movingTimeLine = true
            if (convertEventX(eventX) - pianoKeyWidth > 0) {
                lineOnTime = convertEventX(eventX) - pianoKeyWidth
            } else {
                lineOnTime = 0f
            }
        }

        // piano keys
        var left = pianoKeys.first().left
        var right = pianoKeys.first().right
        top = pianoKeys.last().top
        bottom = pianoKeys.first().bottom

        if (convertEventX(eventX) >= left && convertEventX(eventX) <= right) {
            pianoKeys.forEachIndexed { i, it ->
                if (it.contains(convertEventX(eventX), convertEventY(eventY))) {
                    GlobalScope.launch {
                        midiPlayer.playNote(i.toByte())
                        delay(500)
                        midiPlayer.stopNote(i.toByte())
                    }
                }
            }
        }
    }

    private fun onScrollingEvent(eventX1: Float, eventY1: Float, eventX2: Float, eventY2: Float) {
        // notes
        if (isEditing && movingNote && selectedNotes.isNotEmpty()) {
            // left right
            selectedNotes[movingNoteIndex].start =
                movingNoteStart + (convertEventX(eventX2) - convertEventX(eventX1)).toInt()
            if (selectedNotes[movingNoteIndex].start < 0) {
                selectedNotes[movingNoteIndex].start = 0
            }

            selectedNotes[movingNoteIndex].start = snapNote(selectedNotes[movingNoteIndex].start)
            // up down
            val newPitch = heightToPitchConverter(convertEventY(eventY2))
            selectedNotes[movingNoteIndex].pitch = newPitch.toByte()
        }
        // timeline
        if (movingTimeLine) {
            if (convertEventX(eventX2) - pianoKeyWidth > 0) {
                lineOnTime = convertEventX(eventX2) - pianoKeyWidth
            } else {
                lineOnTime = 0f
            }
        }
    }

    private fun onLongPressEvent(eventX: Float, eventY: Float) {
        if (!isEditing) {
            activeTrack.getNotes().forEach() {
                if (it.getRectF().contains(convertEventX(eventX), convertEventY(eventY))) {
                    selectedNotes.add(it)
                    changeEditingMode()
                }
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
        onDownEvent(event.x, event.y)
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

    override fun onScroll(
        event1: MotionEvent?,
        event2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        /*println("------- ON SCROLL -------")
        if (event1 != null) {
            println("DOWN - X: " + event1.x + " |Y: " + event1.y)
        }
        println("DOWN - X: " + event2.x + " |Y: " + event2.y)
        println("DISTANCE - X: " + distanceX + " |Y: " + distanceY)*/

        if (isCreating && !creatingNote) {
            if (event1 != null) {
                creatingNoteStart =
                    snapNote((convertEventX(event1.x) - pianoKeyWidth).toInt()).toFloat()
            }
            if (event1 != null) {
                creatingNotePitch = heightToPitchConverter(convertEventY(event1.y))
            }
            creatingNote = true
        }

        if (creatingNote) {
            creatingNoteDuration =
                snapNote((convertEventX(event2.x) - pianoKeyWidth - creatingNoteStart).toInt()).toFloat()
            if (creatingNoteDuration < 0) {
                creatingNoteDuration = 0f
            }
        }

        if (!movingNote && isEditing) {
            selectedNotes.forEachIndexed { index, it ->
                if (event1 != null) {
                    if (it.getRectF().contains(convertEventX(event1.x), convertEventY(event1.y))) {
                        movingNoteIndex = index
                        movingNoteStart = it.start
                        movingNotePitch = it.pitch.toInt()
                        movingNote = true
                    }
                }
            }
        }

        if (!scaling && !movingTimeLine && !movingNote && !creatingNote) {
            scrollX += distanceX / scaleFactorX
            scrollY += distanceY / scaleFactorY
        }

        if (event1 != null) {
            onScrollingEvent(event1.x, event1.y, event2.x, event2.y)
        }
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        /*println("------- ON LONG PRESS -------")
        println("X: " + event.x + " |Y: " + event.y)*/

        onLongPressEvent(event.x, event.y)
    }

    override fun onFling(
        p0: MotionEvent?,
        eventDown: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
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

    public fun saveProject() {
        val projectManager = ProjectManager()
        projectManager.saveProjectToFile(project, context)
    }

    public fun loadProject(project: Project) {
        clearAll()
        this.upperTimeSignature = project.getTimeSignatureUpper().toFloat()
        this.lowerTimeSignature = project.getTimeSignatureLower().toFloat()
        this.barLength = (upperTimeSignature / lowerTimeSignature) * 4 * beatLength
        this.tempo = project.getTempo()
        this.project = project
        this.a4Height = project.getPitchOfA1().toFloat()
        val tracks = project.getTracks()
        if (tracks.isNotEmpty()) {
            activeTrack = tracks[0]
            activeTrackIndex = 0
        } else {
            activeTrack = Track()
            activeTrackIndex = -1
        }
    }

    public fun clearAll() {
        selectedNotes.clear()
        movingNoteIndex = -1
        movingNote = false
        playingNotes.clear()
        activeTrack = Track()
        activeTrackIndex = -1
        project = Project()
        a4Height = 442f
        tempo = 120
        upperTimeSignature = 4f
        lowerTimeSignature = 4f
        barLength = (upperTimeSignature / lowerTimeSignature) * 4 * beatLength
    }

    public fun deleteEditedNotes() {
        activeTrack.getNotes().removeAll(selectedNotes.toSet())
        selectedNotes.clear()
        isEditing = false
        redrawAll()
    }

    public fun cancelEditing() {
        selectedNotes.clear()
        isEditing = false
        redrawAll()
    }

    private fun createNewNote() {
        if (creatingNoteDuration == 0f) {
            return
        }

        val rectF = getRectFromNoteInfo(
            creatingNotePitch.toByte(),
            creatingNoteStart.toInt(),
            creatingNoteDuration.toInt()
        )

        val newNote = Note(
            creatingNotePitch.toByte(),
            creatingNoteStart.toInt(),
            creatingNoteDuration.toInt(),
            rectF.left,
            rectF.top,
            rectF.right,
            rectF.bottom
        )

        activeTrack.getNotes().add(newNote)
        redrawAll()
    }

    public fun saveNewSettings(projectSettingsData: ProjectSettingsData) {
        if (projectSettingsData.bpm != null) {
            tempo = projectSettingsData.bpm
        }

        if (projectSettingsData.timeSignatureNumerator != null) {
            upperTimeSignature = projectSettingsData.timeSignatureNumerator.toFloat()
        }

        if (projectSettingsData.timeSignatureDenominator != null) {
            lowerTimeSignature = projectSettingsData.timeSignatureDenominator.toFloat()
        }

        if (projectSettingsData.pitchOfA1 != null) {
            a4Height = projectSettingsData.pitchOfA1.toFloat()
        }

        projectSettingsData.projectName?.let { project.setName(it) }

        barLength = (upperTimeSignature / lowerTimeSignature) * 4 * beatLength
        redrawAll()
    }

    public fun getProjectSettings(): ProjectSettingsData {
        return ProjectSettingsData(
            PitchDetectionMethod.AUTOCORRELATION.name,
            a4Height.toInt(),
            tempo,
            upperTimeSignature.toInt(),
            lowerTimeSignature.toInt(),
            project.getName()
        )
    }

    private fun updateButtonsInFragment() {
        fragment.updateButtons()
    }

    public fun setFragment(fragment: PianoRollFragment) {
        this.fragment = fragment
    }

    public fun startCreatingNotes() {
        if (activeTrackIndex < 0) {
            return
        }
        cancelEditing()
        selectedNotes.clear()
        isCreating = true
        redrawAll()
    }

    public fun stopCreatingNotes() {
        isCreating = false
        redrawAll()
    }

    private fun snapNote(start: Int): Int {
        val snappedStart = start - (start % getSnapLength())
        return snappedStart
    }

    private fun getSnapLength(): Int {
        return 120
    }

    public fun setPitchDetectionMethod(method: PitchDetectionMethod) {
        pitchDetectionMethod = method
    }

    public fun nextTrack() {
        val tracks = project.getTracks()
        val index = tracks.indexOf(activeTrack)
        if (index < tracks.size - 1) {
            activeTrack = tracks[index + 1]
            activeTrackIndex = index
            selectedNotes.clear()
            redrawAll()
        }
    }

    public fun previousTrack() {
        val tracks = project.getTracks()
        val index = tracks.indexOf(activeTrack)
        if (index > 0) {
            activeTrack = tracks[index - 1]
            activeTrackIndex = index
            selectedNotes.clear()
            redrawAll()
        }
    }

    public fun deleteActiveTrack() {
        val tracks = project.getTracks()
        val index = tracks.indexOf(activeTrack)
        if (tracks.size == 1) {
            tracks.clear()
            activeTrack = Track()
            activeTrackIndex = -1
            redrawAll()
            return
        }

        if (tracks.size == index + 1) {
            activeTrack = tracks[index - 1]
            activeTrackIndex = index - 1
            selectedNotes.clear()
            project.removeTrack(tracks[index])
        } else {
            project.removeTrack(tracks[index])
            activeTrack = tracks[index]
            activeTrackIndex = index
            selectedNotes.clear()
        }

        redrawAll()
    }

    public fun canGoToNextTrack(): Boolean {
        val tracks = project.getTracks()
        val index = tracks.indexOf(activeTrack)
        return index < tracks.size - 1
    }

    public fun canGoToPreviousTrack(): Boolean {
        val tracks = project.getTracks()
        val index = tracks.indexOf(activeTrack)
        return index > 0
    }

    public fun canDeleteActiveTrack(): Boolean {
        val return1 = project.getTracks().size > 0
        return return1
    }

    public fun canEditActiveTrackName(): Boolean {
        return activeTrackIndex >= 0
    }

    public fun getActiveTrackName(): String {
        if (activeTrackIndex < 0) {
            return "(No track)"
        }
        return activeTrack.getName()
    }

    public fun setActiveTrackName(name: String) {
        activeTrack.setName(name)
    }

    public fun getActiveTrackIndex(): Int {
        return activeTrackIndex
    }

    public fun isPlayingRecordings(): Boolean {
        return playRecordings
    }

    public fun setPlayRecordings(playRecordings: Boolean) {
        this.playRecordings = playRecordings
    }

    public fun getProject(): Project {
        return project
    }
}