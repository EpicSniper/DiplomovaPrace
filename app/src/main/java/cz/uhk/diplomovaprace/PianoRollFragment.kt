package cz.uhk.diplomovaprace

import ProjectSettingsFragment
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import cz.uhk.diplomovaprace.PianoRoll.PianoRollView
import cz.uhk.diplomovaprace.Project.ProjectViewModel
import cz.uhk.diplomovaprace.Settings.ProjectSettingsData


/**
 * A simple [Fragment] subclass.
 * Use the [PianoRollFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PianoRollFragment : Fragment(), ProjectSettingsFragment.ProjectSettingsDialogListener {

    private var projectSettingsFragment = ProjectSettingsFragment()

    private lateinit var pianoRollView: PianoRollView
    private lateinit var playButton: ImageView
    private lateinit var recordButton: ImageView
    private lateinit var stopButton: ImageView
    private lateinit var deleteEditedButton: ImageView
    private lateinit var cancelEditButton: ImageView

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

        playButton.alpha = 1f
        recordButton.alpha = 1f
        stopButton.alpha = 0.3f
        deleteEditedButton.alpha = 0.3f
        cancelEditButton.alpha = 0.3f

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
        }

        projectSettingsFragment.setListener(this)

        val menuButton = view.findViewById<ImageView>(R.id.pianoRollMenu) // Replace with your menu button ID
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
                        projectSettingsFragment = ProjectSettingsFragment.newInstance(projectSettingsData)
                        projectSettingsFragment.setListener(this)
                        projectSettingsFragment.show(parentFragmentManager, "project_settings")
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

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigate(R.id.mainMenuFragment)
        }
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
    }

    public fun updateButtons() {
        updateRecordButtonStates()
        updateEditButtonStates()
    }

    override fun onSettingsSaved(projectSettingsData: ProjectSettingsData) {
        pianoRollView.saveNewSettings(projectSettingsData)
    }
}