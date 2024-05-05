package cz.uhk.diplomovaprace.PianoRoll

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Canvas.VertexMode
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.AttributeSet
import android.view.*
import android.view.GestureDetector.OnGestureListener
import androidx.core.content.ContextCompat
import cz.uhk.diplomovaprace.PianoRoll.Midi.MidiCreator
import cz.uhk.diplomovaprace.PianoRoll.Midi.MidiFactory
import cz.uhk.diplomovaprace.PianoRoll.Midi.MidiPlayer
import cz.uhk.diplomovaprace.PianoRoll.Midi.Track
import cz.uhk.diplomovaprace.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.*
import kotlin.math.pow

class PianoRollView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback, OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {

    private var paint = Paint()
    private var notes = ArrayList<Note>()
    private var selectedNotes = ArrayList<Note>()
    private var playingNotes = ArrayList<Note>()
    private var pianoKeys = ArrayList<RectF>()  // TODO: vyuzit tento ArrayList
    private var playingPianoKeys = ArrayList<Int>() // onTap only
    private var buttons = ArrayList<RectF>() // 0: play; 1: record; 2: stop;
    // TODO: vsechno dat do ArrayList()<RectF> -> pohlidam si tim klikani, vim, kde co je

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

    private var barTimeSignature = 4 / 4f
    private var beatLength = 480
    private var barLength = barTimeSignature * 4 * beatLength
    private var tempo = 60

    private var isPlaying = false
    private var lineOnTime = 0f
    private var movingTimeLine = false
    private var elapsedTime = System.currentTimeMillis()
    private var lastFrameTime = System.currentTimeMillis()
    private var currentTime = System.currentTimeMillis()
    private var midiPlayer = MidiPlayer()

    private var isRecording = false
    private var recordingLineTime = ArrayList<Float>()
    private var recordingLineAutocorrelation = ArrayList<Double>()
    private var noteHeights = ArrayList<Float>()
    private var randomCounter = 0

    private var isEditing = false

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

        public fun stopDrawing() {
            isPlaying = false
            invalidate()
        }
    }

    private inner class RecordThread() : Thread() {
        private var audioSource = MediaRecorder.AudioSource.MIC
        private var sampleRate = 44100
        private var channelConfig = AudioFormat.CHANNEL_IN_MONO
        private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
        private var bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        @SuppressLint("MissingPermission")
        private var audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
        private var audioData = ShortArray(bufferSize)

        @SuppressLint("RestrictedApi", "MissingPermission")
        override fun start() {
            super.start()

            audioSource = MediaRecorder.AudioSource.MIC
            sampleRate = 44100
            channelConfig = AudioFormat.CHANNEL_IN_MONO
            audioFormat = AudioFormat.ENCODING_PCM_16BIT
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            audioData = ShortArray(bufferSize)
            audioRecord.startRecording()
        }

        override fun run() {
            while (isRecording) {
                val buffer = ShortArray(bufferSize)
                val samplesRead = audioRecord.read(buffer, 0, buffer.size)

                recordingLineTime.add(lineOnTime)
                recordingLineAutocorrelation.add(getAutocorrelationPitch(buffer, sampleRate))

                invalidate()
            }
        }

        public fun stopRecording() {
            isRecording = false
            audioRecord.stop()
            audioRecord.release()
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
        timelineHeight = height / 20f / scaleFactorY            // TODO: Aby uzivatel tuto promennou mohl menit?
        pianoKeyWidth = width / 7f / scaleFactorX               // TODO: Aby uzivatel tuto promennou mohl menit?
        pianoKeyHeight = (height - timelineHeight) / 128f
        pianoKeyBorder = pianoKeyHeight / 20f

        barTimeSignature = 4 / 4f
        beatLength = 480
        barLength = barTimeSignature * 4 * beatLength
        tempo = 60

        isPlaying = false
        lineOnTime = 0f
        movingTimeLine = false
        elapsedTime = System.currentTimeMillis()
        lastFrameTime = System.currentTimeMillis()
        currentTime = System.currentTimeMillis()

        isRecording = false
        randomCounter = 0

        isEditing = false

        rectFArrayListInicialization()

        debugAddNotes()

        midiPlayer = MidiPlayer()

        /*if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1)
        } else {
            // Permission already granted
            // Write your file-saving code here
        }*/

        val midiFactory = MidiFactory()
        midiFactory.main(context)
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
        drawRecordingLine(canvas)

        //drawDebugLines(canvas)

        // playing
        if (isPlaying) {
            currentTime = System.currentTimeMillis()
            elapsedTime = currentTime - lastFrameTime
            lineOnTime += ((tempo / 60f) * beatLength) * elapsedTime / 1000f
            lastFrameTime = currentTime
            playNotes()
        }

        var midiCreator = MidiCreator()
        var track = Track()
        track.setNotes(notes)
        midiCreator.addTrack(track)
        var midiData = midiCreator.createMidiData(context,4,4, tempo)

        setHertzToNotes(442f)
        onCreateTestFunction()
        canvas.restore()
        unlockCanvas(canvas)
    }

    public fun stopPlaying() {
        isPlaying = false
    }

    private fun onCreateTestFunction() {

    }

    public fun changeEditingMode() {
        isEditing = !isEditing
        if (!isEditing) {
            selectedNotes.clear()
        }

        invalidate()
    }

    // Napiš mi funkci na PWPD Algoritmus
    // Výsledkem bude výška tónu v hertzích
    private fun getPitchPWPD(audioData: ShortArray, sampleRate: Int): Double {
        val numSamples = audioData.size
        val audioDataDouble = DoubleArray(numSamples)

        // Convert audio samples to array of doubles between -1 and 1
        for (i in 0 until numSamples) {
            audioDataDouble[i] = audioData[i] / 32768.0 // 32768.0 is the maximum value of a signed 16-bit integer
        }

        // Apply FFT to get frequency spectrum
        val fft = DoubleFFT_1D(numSamples.toLong())
        fft.realForward(audioDataDouble)

        // Calculate magnitude spectrum
        val magnitudeSpectrum = DoubleArray(numSamples / 2)
        for (i in 0 until numSamples / 2) {
            val real = audioDataDouble[2 * i]
            val imag = audioDataDouble[2 * i + 1]
            magnitudeSpectrum[i] = sqrt(real * real + imag * imag)
        }

        // Apply Harmonic Product Spectrum method
        val maxHarmonics = 5
        val hps = DoubleArray(numSamples / (2 * maxHarmonics))
        for (i in 0 until hps.size) {
            hps[i] = magnitudeSpectrum[i]
            for (j in 2..maxHarmonics) {
                if (i * j < magnitudeSpectrum.size) {
                    hps[i] *= magnitudeSpectrum[i * j]
                } else {
                    hps[i] = 0.0
                    break
                }
            }
        }

        // Find the peak in the HPS array
        var maxHps = hps.maxOrNull() ?: 0.0
        var maxHpsIndex = hps.toList().indexOf(maxHps)

        // Calculate pitch in Hz
        return sampleRate.toDouble() * maxHpsIndex / numSamples
    }

    private fun getPitchMLE(audioData: ShortArray, sampleRate: Int): Double {
        val numSamples = audioData.size
        val audioDataDouble = DoubleArray(numSamples)

        // Convert audio samples to array of doubles between -1 and 1
        for (i in 0 until numSamples) {
            audioDataDouble[i] = audioData[i] / 32768.0 // 32768.0 is the maximum value of a signed 16-bit integer
        }

        // Apply FFT to get frequency spectrum
        val fft = DoubleFFT_1D(numSamples.toLong())
        fft.realForward(audioDataDouble)

        // Calculate magnitude spectrum
        val magnitudeSpectrum = DoubleArray(numSamples / 2)
        for (i in 0 until numSamples / 2) {
            val real = audioDataDouble[2 * i]
            val imag = audioDataDouble[2 * i + 1]
            magnitudeSpectrum[i] = sqrt(real * real + imag * imag)
        }

        // Apply Harmonic Product Spectrum method
        val maxHarmonics = 5
        val hps = DoubleArray(numSamples / (2 * maxHarmonics))
        for (i in 0 until hps.size) {
            hps[i] = magnitudeSpectrum[i]
            for (j in 2..maxHarmonics) {
                if (i * j < magnitudeSpectrum.size) {
                    hps[i] *= magnitudeSpectrum[i * j]
                } else {
                    hps[i] = 0.0
                    break
                }
            }
        }

        // Find the peak in the HPS array
        var maxHps = hps.maxOrNull() ?: 0.0
        var maxHpsIndex = hps.toList().indexOf(maxHps)

        // Calculate pitch in Hz
        return sampleRate.toDouble() * maxHpsIndex / numSamples
    }

    private fun getPitch(audioData: ShortArray, sampleRate: Int): Double {
        val numSamples = audioData.size
        val audioDataDouble = DoubleArray(numSamples)

        // Convert audio samples to array of doubles between -1 and 1
        for (i in 0 until numSamples) {
            audioDataDouble[i] = audioData[i] / 32768.0 // 32768.0 is the maximum value of a signed 16-bit integer
        }

        // Apply FFT to get frequency spectrum
        val fft = DoubleFFT_1D(numSamples.toLong())
        fft.realForward(audioDataDouble)

        // Calculate magnitude spectrum
        val magnitudeSpectrum = DoubleArray(numSamples / 2)
        for (i in 0 until numSamples / 2) {
            val real = audioDataDouble[2 * i]
            val imag = audioDataDouble[2 * i + 1]
            magnitudeSpectrum[i] = sqrt(real * real + imag * imag)
        }

        // Apply Harmonic Product Spectrum method
        val maxHarmonics = 5
        val hps = DoubleArray(numSamples / (2 * maxHarmonics))
        for (i in 0 until hps.size) {
            hps[i] = magnitudeSpectrum[i]
            for (j in 2..maxHarmonics) {
                if (i * j < magnitudeSpectrum.size) {
                    hps[i] *= magnitudeSpectrum[i * j]
                } else {
                    hps[i] = 0.0
                    break
                }
            }
        }

        // Find the peak in the HPS array
        var maxHps = hps.maxOrNull() ?: 0.0
        var maxHpsIndex = hps.toList().indexOf(maxHps)

        // Calculate pitch in Hz
        return sampleRate.toDouble() * maxHpsIndex / numSamples
    }

    private fun getAutocorrelationPitch(audioData: ShortArray, sampleRate: Int): Double {
        if (false) {
            val numSamples = audioData.size
            val audioDataDouble = DoubleArray(numSamples)

            // Convert audio samples to array of doubles between -1 and 1
            for (i in 0 until numSamples) {
                audioDataDouble[i] =
                    audioData[i] / 32768.0 // 32768.0 is the maximum value of a signed 16-bit integer
            }

            // Apply FFT to get frequency spectrum
            val fft = DoubleFFT_1D(numSamples.toLong())
            fft.realForward(audioDataDouble)

            // Calculate magnitude spectrum
            val magnitudeSpectrum = DoubleArray(numSamples / 2)
            for (i in 0 until numSamples / 2) {
                val real = audioDataDouble[2 * i]
                val imag = audioDataDouble[2 * i + 1]
                magnitudeSpectrum[i] = sqrt(real * real + imag * imag)
            }

            // Apply Harmonic Product Spectrum method
            val maxHarmonics = 5
            val hps = DoubleArray(numSamples / (2 * maxHarmonics))
            for (i in 0 until hps.size) {
                hps[i] = magnitudeSpectrum[i]
                for (j in 2..maxHarmonics) {
                    if (i * j < magnitudeSpectrum.size) {
                        hps[i] *= magnitudeSpectrum[i * j]
                    } else {
                        hps[i] = 0.0
                        break
                    }
                }
            }

            // Find the peak in the HPS array
            var maxHps = hps.maxOrNull() ?: 0.0
            var maxHpsIndex = hps.toList().indexOf(maxHps)

            // Calculate pitch in Hz
            return sampleRate.toDouble() * maxHpsIndex / numSamples
        } else {
            val numSamples = audioData.size
            val audioDataDouble = DoubleArray(numSamples)

            // Convert audio samples to array of doubles between -1 and 1
            for (i in 0 until numSamples) {
                audioDataDouble[i] = audioData[i] / 32768.0 // 32768.0 is the maximum value of a signed 16-bit integer
            }

            val minPeriod = (sampleRate / 2000) // Minimum period for pitch detection (e.g., 1000 Hz)
            val maxPeriod = (sampleRate / 80) // Maximum period for pitch detection (e.g., 200 Hz)

            var pitchPeriod = 0
            var maxCorrelation = 0.0

            // Calculate autocorrelation for different pitch periods
            for (period in minPeriod until maxPeriod) {
                var correlation = 0.0
                for (i in 0 until numSamples - period) {
                    correlation += audioDataDouble[i] * audioDataDouble[i + period]
                }

                if (correlation > maxCorrelation) {
                    maxCorrelation = correlation
                    pitchPeriod = period
                }
            }

            // Calculate pitch in Hz
            return sampleRate.toDouble() / pitchPeriod.toDouble()
        }
    }

    private fun calculateRMS(audioData: DoubleArray): Double {
        var sum = 0.0
        for (value in audioData) {
            sum += value * value
        }
        val meanSquare = sum / audioData.size
        return sqrt(meanSquare)
    }

    // a4Height - default 442Hz
    private fun setHertzToNotes(a4Height: Float) {
        var noteArray = ArrayList<Float>()
        for (i in 0 until 128) {
            noteArray.add(a4Height * 2f.pow((i-69f)/12f))
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
        canvas.drawLine(lineOnTime + pianoKeyWidth, 0f, lineOnTime + pianoKeyWidth, height.toFloat(), paint)
        paint.strokeWidth = 0f
    }

    private fun drawRecordingLine(canvas: Canvas) {
        if (recordingLineTime.size >= recordingLineAutocorrelation.size) {
            for (i in 0 until recordingLineAutocorrelation.size) {
                if (i != 0) {
                    paint.color = Color.GREEN
                    canvas.drawLine(
                        recordingLineTime[i - 1],
                        recordingLineAutocorrelation[i - 1].toFloat(),
                        recordingLineTime[i],
                        recordingLineAutocorrelation[i].toFloat(),
                        paint
                    )
                }
            }
        }
    }

    private fun convertRecordingToNotes() {
        var recordedNotes = ArrayList<Int>()
        var newNotes = ArrayList<Note>()
        recordingLineAutocorrelation.forEach {
            recordedNotes.add(closestNote(it.toFloat()))
        }

        var lastNote = 0
        var startOfTheLastNote = 0
        recordedNotes.forEachIndexed { i, note ->
            if (i != 0) {
                if (lastNote != note) {
                    val pitch = note.toByte()
                    val start = startOfTheLastNote
                    val duration = recordingLineTime[i].toInt() - startOfTheLastNote
                    val rectF = getRectFromNoteInfo(pitch, start,duration)
                    newNotes.add(Note(pitch, start, duration, rectF))
                    lastNote = note
                    startOfTheLastNote = recordingLineTime[i].toInt()
                }
            } else {
                lastNote = note
                startOfTheLastNote = recordingLineTime[i].toInt()
            }
        }

        val pitch = lastNote.toByte()
        val start = startOfTheLastNote
        val duration = recordingLineTime.last().toInt() - startOfTheLastNote
        val rectF = getRectFromNoteInfo(pitch, start,duration)
        newNotes.add(Note(pitch, start, duration, rectF))

        newNotes.forEach {
            notes.add(it)
        }

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

    private fun drawTimelineAndPiano(canvas: Canvas)  {
        // draw timeline
        paint.color = ContextCompat.getColor(context, R.color.pianorollframe)
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
        var upperColor = ContextCompat.getColor(context, R.color.pinkie)
        var bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
        val upperLineThickness = 2f
        val bottomLineThickness = 1f

        paint.textScaleX = scaleFactorY / scaleFactorX
        var barNumberCorrection = 1 - (pianoKeyWidth / barLength).toInt()
        do {
            var renderLines = true
            // vykreslit vsechny cary
            when (sixteenthLengths % 16) {
                0 -> {
                    topOfTheLine = top
                    upperColor = ContextCompat.getColor(context, R.color.text1)
                    bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    paint.textSize = timelineHeight / 4f
                    paint.color = upperColor
                    canvas.drawText(((actualTime / barLength).toInt() + barNumberCorrection).toString(), actualTime + 5, top + timelineHeight / 4f, paint)
                }

                1, 3, 5, 7, 9, 11, 13, 15 -> {
                    if (scaleFactorX > 0.32f) {
                        topOfTheLine = top + (timelineHeight / 16f * 12f )
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    } else {
                        renderLines = false
                    }
                }

                2, 6, 10, 14 -> {
                    if (scaleFactorX > 0.16f) {
                        topOfTheLine = top + (timelineHeight / 16f * 11f )
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    } else {
                        renderLines = false
                    }
                }

                4, 12 -> {
                    if (scaleFactorX > 0.08f) {
                        topOfTheLine = top + (timelineHeight / 16f * 10f )
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    } else {
                        renderLines = false
                    }

                }

                8 -> {
                    if (scaleFactorX > 0.04f) {
                        topOfTheLine = top + (timelineHeight / 16f * 8f )
                        upperColor = ContextCompat.getColor(context, R.color.text1)
                        bottomColor = ContextCompat.getColor(context, R.color.pianorollline)
                    } else {
                        renderLines = false
                    }
                }
            }

            if (renderLines) {
                paint.color = upperColor
                paint.strokeWidth = upperLineThickness
                canvas.drawLine(actualTime, topOfTheLine, actualTime, bottom, paint)
                paint.strokeWidth = bottomLineThickness
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
        inicializeButtons()
        inicializePianoKeys()
    }

    private fun inicializeButtons() {
        buttons.add(0, RectF(0f, 0f, 0f, 0f))   // index 0: play button
        buttons.add(1, RectF(0f, 0f, 0f, 0f))   // index 1: record button
        buttons.add(2, RectF(0f, 0f, 0f, 0f))   // index 2: stop button
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
        // Setting top and bottom pixels of buttons
        val top = scrollY + heightDifference / 2f
        val buttonBottom = top + (height - height / 30f) / scaleFactorY
        val buttonTop = top + (height - height / 10f) / scaleFactorY
        val buttonHeight = (buttonBottom - buttonTop) * scaleFactorY / scaleFactorX
        val buttonCenterY = buttonTop + (buttonBottom - buttonTop) / 2f
        val playButtonLeft = (width / 2f) - (buttonHeight / 2f) + scrollX
        val playButtonRight = (width / 2f) + (buttonHeight / 2f) + scrollX

        buttons[0] = RectF(playButtonLeft, buttonTop, playButtonRight, buttonBottom)
        buttons[1] = RectF(playButtonLeft - buttonHeight * 1.2f, buttonTop, playButtonRight - buttonHeight * 1.2f, buttonBottom)
        buttons[2] = RectF(playButtonLeft + buttonHeight * 1.2f, buttonTop, playButtonRight + buttonHeight * 1.2f, buttonBottom)

        if (isPlaying || isRecording) {
            paint.color = Color.GRAY                    // TODO: color
            canvas.drawOval(buttons[0], paint)          // play button background
            canvas.drawOval(buttons[1], paint)          // record button background
            paint.color = Color.WHITE                   // TODO: color
            canvas.drawOval(buttons[2], paint)          // stop button background
        } else {
            paint.color = Color.WHITE                   // TODO: color
            canvas.drawOval(buttons[0], paint)          // play button background
            canvas.drawOval(buttons[1], paint)          // record button background
            paint.color = Color.GRAY                    // TODO: color
            canvas.drawOval(buttons[2], paint)          // stop button background
        }

        // draw play button symbol
        val heightCorrection = (buttonBottom - buttonTop) / 5f
        val widthCorrection = (playButtonRight - playButtonLeft) / 4f;
        val triangleVerticies = floatArrayOf(
            playButtonLeft + widthCorrection, buttonTop + heightCorrection,  // top vertex
            playButtonLeft + widthCorrection, buttonBottom - heightCorrection,  // bottom vertex
            playButtonRight - widthCorrection, buttonCenterY   // right vertex
        )

        val colors = intArrayOf(
            Color.BLACK, Color.BLACK, Color.BLACK, -0x1000000, -0x1000000, -0x1000000         // TODO: color
        )

        val vertexCount = triangleVerticies.size
        canvas.drawVertices(
            VertexMode.TRIANGLES, vertexCount, triangleVerticies,
            0,null,0,
            colors.map { it.toInt() }.toIntArray(),
            0, null,0, 0, paint
        )

        // draw record button symbol
        paint.color = Color.RED
        var symbolHeightCorrection = (buttonBottom - buttonTop) / 3.5f
        var symbolWidthCorrection = (playButtonRight - playButtonLeft) / 3.5f
        canvas.drawOval(buttons[1].left + symbolWidthCorrection,
            buttons[1].top + symbolHeightCorrection,
            buttons[1].right - symbolWidthCorrection,
            buttons[1].bottom - symbolHeightCorrection, paint)

        paint.color = Color.BLACK
        symbolHeightCorrection = (buttonBottom - buttonTop) / 3.2f
        symbolWidthCorrection = (playButtonRight - playButtonLeft) / 3.2f
        canvas.drawRect(buttons[2].left + symbolWidthCorrection,
            buttons[2].top + symbolHeightCorrection,
            buttons[2].right - symbolWidthCorrection,
            buttons[2].bottom - symbolHeightCorrection, paint)

        // TODO: edit button
    }

    // TODO: Budou potreba tyto metody pro konvert?
    private fun pitchToHeightConverter(pitch: Int): Float {
        return height / 2f  // FIXME: placeholder
    }

    private fun heightToPitchConverter(height: Float): Int {
        return 60           // FIXME: placeholder
    }

    private fun pitchToNameConverter(pitch: Int): String {
        var noteName = ""

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
            drawNote(canvas, it)
        }
    }

    private fun drawNote(canvas: Canvas, note: Note) {
        // Namalovat okraje
        var noteRectF = note.rectF

        paint.color = ContextCompat.getColor(context, R.color.note)
        // Namalovat vnitrek
        selectedNotes.forEach {
            if (it == note) {
                paint.color = ContextCompat.getColor(context, R.color.pinkie)
            }
        }

        noteRectF = RectF(noteRectF.left + pianoKeyBorder, noteRectF.top + pianoKeyBorder, noteRectF.right - pianoKeyBorder, noteRectF.bottom - pianoKeyBorder)
        val cornerRadiusX = (noteRectF.bottom - noteRectF.top) / 4f
        val cornerRadiusY = (noteRectF.bottom - noteRectF.top) * 2f / scaleFactorX
        canvas.drawRoundRect(noteRectF, cornerRadiusY, cornerRadiusX, paint)
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
            val cornerRadiusX = (it.bottom - it.top) / 5f
            val cornerRadiusY = (it.right - it.left) / 5f
            canvas.drawRoundRect(rect, cornerRadiusY, cornerRadiusX, paint)

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
            canvas.drawText(keyText, it.left + textWidthPadding, it.bottom - textHeightPadding, paint)

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
        notes.forEach {
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

    fun getRectFromNoteInfo(pitch: Byte, start: Int, duration: Int): RectF {
        var bottom = height - (pitch * pianoKeyHeight)
        var top = bottom - pianoKeyHeight
        var left = start + pianoKeyWidth       // Posunuji o sirku klaves
        var right = left + duration
        return RectF(left, top, right, bottom)
    }

    private fun buttonsOnSingleTapUpEvent(eventX: Float, eventY: Float) {
        // buttons[0] - play button
        // buttons[1] - recording button
        // buttons[2] - stop button
        if(buttons[0].contains(eventX, eventY)) {
            if (!isPlaying && !isRecording) {
                isPlaying = true
                drawThread = DrawThread()
                drawThread?.start()
                resetTime()
                midiPlayer.onMidiStart()
            }
        } else if (buttons[1].contains(eventX, eventY)) {
            if (!isRecording && !isPlaying) {
                isRecording = true
                recordThread = RecordThread()
                recordThread?.start()

                isPlaying = true
                drawThread = DrawThread()
                drawThread?.start()
                resetTime()
                midiPlayer.onMidiStart()
            }
        } else if (buttons[2].contains(eventX, eventY)) {
            if (isPlaying) {
                isPlaying = false
                drawThread?.stopDrawing()
                drawThread = null
                playingNotes.clear()
                midiPlayer.stopAllNotes()
            }

            if (isRecording) {
                isRecording = false
                recordThread?.stopRecording()
                recordThread = null
            }
        }
    }

    private fun onSingleTapUpEvent(eventX: Float, eventY: Float) {
        if (isEditing) {
            notes.forEach {
                if (it.rectF.contains(eventX, eventY)) {
                    if (selectedNotes.contains(it)) {
                        selectedNotes.remove(it)
                    } else {
                        selectedNotes.add(it)
                        midiPlayer.playNote(it.pitch)
                        midiPlayer.stopNote(it.pitch)
                    }
                }
            }
        } else {
            buttonsOnSingleTapUpEvent(eventX, eventY)
        }
    }

    private fun onDownEvent(eventX: Float, eventY: Float) {
        // timeline
        var top = scrollY + heightDifference / 2f
        var bottom = top + timelineHeight
        movingTimeLine = false
        if (convertEventY(eventY) > top && convertEventY(eventY) <= bottom) {
            // taplo se na timeline -> presunout line
            isPlaying = false
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
            pianoKeys.forEachIndexed {i, it ->
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
        // timeline
        if (movingTimeLine) {
            if (convertEventX(eventX2) - pianoKeyWidth > 0) {
                lineOnTime = convertEventX(eventX2) - pianoKeyWidth
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

    override fun onScroll(event1: MotionEvent?, event2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        /*println("------- ON SCROLL -------")
        println("DOWN - X: " + event1.x + " |Y: " + event1.y)
        println("DOWN - X: " + event2.x + " |Y: " + event2.y)
        println("DISTANCE - X: " + distanceX + " |Y: " + distanceY)*/
        if (!scaling && !movingTimeLine) {
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
        var note = Note(60, 0,480, rectF)
        notes.add(note)

        rectF = getRectFromNoteInfo(64, 240,480)
        note = Note(64, 240,960, rectF)
        notes.add(note)

        rectF = getRectFromNoteInfo(64, 1440,960)
        note = Note(60, 1440,480, rectF)
        notes.add(note)

        rectF = getRectFromNoteInfo(64, 1440,960)
        note = Note(64, 1440,960, rectF)
        notes.add(note)
    }
}