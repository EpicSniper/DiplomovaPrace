package cz.uhk.miniMidiStudio.PianoRoll

import ProjectSettingsFragment
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import cz.uhk.miniMidiStudio.PianoRoll.Midi.MidiCreator
import cz.uhk.miniMidiStudio.R
import cz.uhk.miniMidiStudio.Project.ProjectViewModel
import cz.uhk.miniMidiStudio.Settings.ProjectSettingsData

class PianoRollFragment : Fragment(), ProjectSettingsFragment.ProjectSettingsDialogListener {

    private var projectSettingsFragment = ProjectSettingsFragment()

    private lateinit var pianoRollView: PianoRollView
    private lateinit var playButton: ImageView
    private lateinit var recordButton: ImageView
    private lateinit var stopButton: ImageView
    private lateinit var deleteEditedButton: ImageView
    private lateinit var cancelEditButton: ImageView
    private lateinit var createButton: ImageView
    private lateinit var nextTrackButton: ImageView
    private lateinit var previousTrackButton: ImageView
    private lateinit var deleteTrackButton: ImageView
    private lateinit var editTrackNameButton: ImageView
    private lateinit var activeTrackName: TextView
    private lateinit var changePlayingSoundButton: ImageView
    private var activeTrackIndex = -1

    private val viewModel: ProjectViewModel by activityViewModels()

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

        viewModel.selectedProject.observe(viewLifecycleOwner) { project ->
            pianoRollView.loadProject(project)
        }

        pianoRollView.setFragment(this)

        playButton = view.findViewById(R.id.playButton)
        recordButton = view.findViewById(R.id.recordButton)
        stopButton = view.findViewById(R.id.stopButton)
        deleteEditedButton = view.findViewById(R.id.deleteEditedButton)
        cancelEditButton = view.findViewById(R.id.cancelEditButton)
        createButton = view.findViewById(R.id.createButton)
        nextTrackButton = view.findViewById(R.id.nextTrackButton)
        previousTrackButton = view.findViewById(R.id.previousTrackButton)
        deleteTrackButton = view.findViewById(R.id.deleteTrackButton)
        editTrackNameButton = view.findViewById(R.id.editTrackNameButton)
        activeTrackName = view.findViewById(R.id.activeTrackName)
        changePlayingSoundButton = view.findViewById(R.id.changePlayingSoundButton)

        playButton.alpha = 1f
        recordButton.alpha = 1f
        stopButton.alpha = 0.3f
        deleteEditedButton.alpha = 0.3f
        cancelEditButton.alpha = 0.3f
        createButton.setImageResource(R.drawable.plus)
        createButton.alpha = 0.3f
        nextTrackButton.alpha = 0.3f
        previousTrackButton.alpha = 0.3f
        deleteTrackButton.alpha = 0.3f
        editTrackNameButton.alpha = 0.3f
        changePlayingSoundButton.alpha = 1f

        setActiveTrackName()

        playButton.setOnClickListener {
            pianoRollView.pushPlayButton()
            updateRecordButtonStates()
        }

        recordButton.setOnClickListener {
            pianoRollView.pushRecordButton()
            updateRecordButtonStates()
        }

        stopButton.setOnClickListener {
            pianoRollView.pushStopButton()
            updateRecordButtonStates()
            setActiveTrackName()
        }

        projectSettingsFragment.setListener(this)

        val menuButton =
            view.findViewById<ImageView>(R.id.pianoRollMenu) // Replace with your menu button ID
        menuButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.piano_roll_menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save_project -> {
                        pianoRollView.saveProject()
                        true
                    }

                    R.id.action_project_settings -> {
                        val projectSettingsData = pianoRollView.getProjectSettings()
                        projectSettingsFragment =
                            ProjectSettingsFragment.newInstance(projectSettingsData)
                        projectSettingsFragment.setListener(this)
                        projectSettingsFragment.show(parentFragmentManager, "project_settings")
                        true
                    }

                    R.id.action_export -> {
                        val midiCreator = MidiCreator()
                        midiCreator.setProject(pianoRollView.getProject())
                        context?.let { midiCreator.shareMidiFile(it) }
                        true
                    }

                    else -> false
                }
            }
            popupMenu.show()
        }

        deleteEditedButton.setOnClickListener {
            pianoRollView.deleteEditedNotes()
        }

        cancelEditButton.setOnClickListener {
            pianoRollView.cancelEditing()
            updateEditButtonStates()
        }

        createButton.setOnClickListener {
            if (activeTrackIndex == -1) {
                createButton.setImageResource(R.drawable.plus)
                createButton.alpha = 0.3f
            } else {
                if (pianoRollView.isCreating) {
                    stopCreatingNotes()
                } else {
                    startCreatingNotes()
                }
            }
        }

        nextTrackButton.setOnClickListener {
            pianoRollView.nextTrack()
            setActiveTrackName()
        }

        previousTrackButton.setOnClickListener {
            pianoRollView.previousTrack()
            setActiveTrackName()
        }

        deleteTrackButton.setOnClickListener {
            if (pianoRollView.canDeleteActiveTrack()) {
                pianoRollView.deleteActiveTrack()
                setActiveTrackName()
            }
        }

        changePlayingSoundButton.setOnClickListener {
            if (pianoRollView.isPlaying || pianoRollView.isRecording) {
                return@setOnClickListener
            }
            pianoRollView.setPlayRecordings(!pianoRollView.isPlayingRecordings())
            updateRecordingButtons()
        }

        editTrackNameButton.setOnClickListener {
            if (pianoRollView.canEditActiveTrackName()) {
                val dialogView =
                    LayoutInflater.from(context).inflate(R.layout.edit_property_dialog, null)
                val inputEditText =
                    dialogView.findViewById<TextInputEditText>(R.id.propertyValueInput)
                inputEditText.setText(pianoRollView.getActiveTrackName())

                context?.let { it1 ->
                    MaterialAlertDialogBuilder(it1)
                        .setTitle("Edit track name")
                        .setView(dialogView)
                        .setPositiveButton("Edit") { _, _ ->
                            val newTrackName = inputEditText.text.toString()
                            pianoRollView.setActiveTrackName(newTrackName)
                            setActiveTrackName()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                setActiveTrackName()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigate(R.id.mainMenuFragment)
        }
    }

    private fun setActiveTrackName() {
        activeTrackName.text = pianoRollView.getActiveTrackName()
    }

    private fun updateRecordButtonStates() {
        if (pianoRollView.isPlaying || pianoRollView.isRecording) {
            playButton.alpha = 0.3f
            recordButton.alpha = 0.3f
            stopButton.alpha = 1f
        } else {
            playButton.alpha = 1f
            recordButton.alpha = 1f
            stopButton.alpha = 0.3f
        }
    }

    private fun updateEditButtonStates() {
        if (pianoRollView.isEditing) {
            deleteEditedButton.alpha = 1f
            cancelEditButton.alpha = 1f
        } else {
            deleteEditedButton.alpha = 0.3f
            cancelEditButton.alpha = 0.3f
        }

        if (pianoRollView.isCreating) {
            createButton.alpha = 1f
            createButton.setImageResource(R.drawable.plusred)
        } else if (activeTrackIndex == -1) {
            createButton.alpha = 0.3f
            createButton.setImageResource(R.drawable.plus)
        } else {
            createButton.alpha = 1f
            createButton.setImageResource(R.drawable.plus)
        }
    }

    public fun updateButtons() {
        activeTrackIndex = pianoRollView.getActiveTrackIndex()
        updateRecordButtonStates()
        updateEditButtonStates()
        updateTrackButtons()
        updateRecordingButtons()
    }

    private fun updateRecordingButtons() {
        if (pianoRollView.isPlayingRecordings()) {
            changePlayingSoundButton.setImageResource(R.drawable.voice)
        } else {
            changePlayingSoundButton.setImageResource(R.drawable.midi)
        }
    }

    private fun updateTrackButtons() {
        if (pianoRollView.canGoToNextTrack()) {
            nextTrackButton.alpha = 1f
        } else {
            nextTrackButton.alpha = 0.3f
        }

        if (pianoRollView.canGoToPreviousTrack()) {
            previousTrackButton.alpha = 1f
        } else {
            previousTrackButton.alpha = 0.3f
        }

        if (pianoRollView.canDeleteActiveTrack()) {
            deleteTrackButton.alpha = 1f
        } else {
            deleteTrackButton.alpha = 0.3f
        }

        if (pianoRollView.canEditActiveTrackName()) {
            editTrackNameButton.alpha = 1f
        } else {
            editTrackNameButton.alpha = 0.3f
        }
    }

    private fun startCreatingNotes() {
        createButton.alpha = 1f
        createButton.setImageResource(R.drawable.plusred)
        pianoRollView.startCreatingNotes()
    }

    private fun stopCreatingNotes() {
        createButton.alpha = 1f
        createButton.setImageResource(R.drawable.plus)
        pianoRollView.stopCreatingNotes()
    }

    override fun onSettingsSaved(projectSettingsData: ProjectSettingsData) {
        pianoRollView.saveNewSettings(projectSettingsData)
    }
}