package cz.uhk.diplomovaprace

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.viewModels
import cz.uhk.diplomovaprace.PianoRoll.PianoRollView
import cz.uhk.diplomovaprace.Project.ProjectViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [PianoRollFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PianoRollFragment : Fragment() {

    private lateinit var pianoRollView: PianoRollView
    private lateinit var playButton: ImageView
    private lateinit var recordButton: ImageView
    private lateinit var stopButton: ImageView

    private val viewModel: ProjectViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_piano_roll, container, false)
        pianoRollView = view.findViewById(R.id.piano_roll_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set the data for the piano roll view
        viewModel.selectedProject.observe(viewLifecycleOwner) { project ->
            // Inicializujte Piano Roll s datyz projec
            // TODO: v pianorollu napsat funkci na import projektu
            pianoRollView.setTempo(project.getTempo())
            pianoRollView.setTimeSignature(project.getTimeSignatureUpper(), project.getTimeSignatureLower())

            for (track in project.getTracks()) {
                // Vytvořte novou stopu v Piano Rollu pro každý track v projektu
                val pianoRollTrack = pianoRollView.createTrack()

                for (note in track.getNotes()) {
                    // Přidejte noty do stopy v Piano Rollu
                    pianoRollTrack.addNote(note)
                }}
        }

        // Get the buttons from the view
        playButton = view.findViewById(R.id.imageView4)
        recordButton = view.findViewById(R.id.imageView5)
        stopButton = view.findViewById(R.id.imageView6)

        playButton.visibility = View.VISIBLE
        recordButton.visibility = View.VISIBLE
        stopButton.visibility = View.INVISIBLE

        // Set the onClickListeners for the buttons
        playButton.setOnClickListener {
            // Call the play function from PianoRollView
            pianoRollView.pushPlayButton()
            updateButtonStates()
        }

        recordButton.setOnClickListener {
            // Call the record function from PianoRollView
            pianoRollView.pushRecordButton()
            updateButtonStates()
        }

        stopButton.setOnClickListener {
            // Call the stop function from PianoRollView
            pianoRollView.pushStopButton()
            updateButtonStates()
        }
    }

    private fun updateButtonStates() {
        if (pianoRollView.isPlaying || pianoRollView.isRecording) {
            playButton.visibility = View.INVISIBLE
            recordButton.visibility = View.INVISIBLE
            stopButton.visibility = View.VISIBLE
        } else {
            playButton.visibility = View.VISIBLE
            recordButton.visibility = View.VISIBLE
            stopButton.visibility = View.INVISIBLE
        }
    }
}