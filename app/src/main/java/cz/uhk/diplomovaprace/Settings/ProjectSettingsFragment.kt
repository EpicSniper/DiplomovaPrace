import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.uhk.diplomovaprace.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import cz.uhk.diplomovaprace.PianoRoll.PitchDetectionMethod
import cz.uhk.diplomovaprace.Settings.ProjectSettingsData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProjectSettingsFragment : BottomSheetDialogFragment() {

    private var listener: ProjectSettingsDialogListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_project_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val jsonString = arguments?.getString("project")
        val project = if (jsonString != null) {
            Json.decodeFromString<ProjectSettingsData>(jsonString)
        } else {
            null
        }

        val pitchView = view.findViewById<TextView>(R.id.pitch)
        val bpmView = view.findViewById<TextView>(R.id.bpm)
        val timeSignatureNumeratorView = view.findViewById<TextView>(R.id.beatsPerMeausure)
        val timeSignatureDenominatorView = view.findViewById<TextView>(R.id.beatValue)
        val projectNameView = view.findViewById<TextView>(R.id.projectName)

        projectNameView.text = project?.projectName ?: "New project"
        pitchView.text = (project?.pitchOfA1 ?: 440).toString()
        bpmView.text = (project?.bpm ?: 120).toString()
        timeSignatureNumeratorView.text = (project?.timeSignatureNumerator ?: 4).toString()
        timeSignatureDenominatorView.text = (project?.timeSignatureDenominator ?: 4).toString()


        projectNameView.setOnClickListener {
            project?.projectName?.let { it1 ->
                showEditTextDialog("project name", it1) { newValue ->
                    projectNameView.text = newValue
                }
            }
        }

        pitchView.setOnClickListener {
            showEditNumberDialog("pitch of A1", project?.pitchOfA1.toString()) { newValue ->
                pitchView.text = newValue
            }
        }

        bpmView.setOnClickListener {
            showEditNumberDialog("beats per minute", project?.bpm.toString()) { newValue ->
                bpmView.text = newValue
            }
        }

        timeSignatureNumeratorView.setOnClickListener {
            showEditNumberDialog(
                "beats per measure",
                project?.timeSignatureNumerator.toString()
            ) { newValue ->
                timeSignatureNumeratorView.text = newValue
            }
        }

        timeSignatureDenominatorView.setOnClickListener {
            showEditNumberDialog(
                "beat value",
                project?.timeSignatureDenominator.toString()
            ) { newValue ->
                timeSignatureDenominatorView.text = newValue
            }
        }

        val saveButton = view.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            // 1. Gather the settings data from the dialog's UI elements
            val projectSettingsData = ProjectSettingsData(
                algorithmType = PitchDetectionMethod.AUTOCORRELATION.name,
                pitchOfA1 = pitchView.text.toString().toInt(),
                bpm = bpmView.text.toString().toInt(),
                timeSignatureNumerator = timeSignatureNumeratorView.text.toString().toInt(),
                timeSignatureDenominator = timeSignatureDenominatorView.text.toString().toInt(),
                projectName = projectNameView.text.toString()
            )

            // 2. Trigger the callback to pass the data to the PianoRollFragment
            listener?.onSettingsSaved(projectSettingsData)

            // 3. Optionally, dismiss the dialog
            dismiss()
        }
    }

    fun setListener(listener: ProjectSettingsDialogListener) {
        this.listener = listener
    }

    private fun showEditTextDialog(title: String, currentValue: String, onSave: (String) -> Unit) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.edit_property_dialog, null)
        val inputEditText = dialogView.findViewById<TextInputEditText>(R.id.propertyValueInput)
        inputEditText.setText(currentValue)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change $title")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newValue = inputEditText.text.toString()
                onSave(newValue)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditNumberDialog(
        title: String,
        currentValue: String,
        onSave: (String) -> Unit
    ) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.edit_property_dialog, null)
        val inputEditText = dialogView.findViewById<TextInputEditText>(R.id.propertyValueInput)
        inputEditText.setText(currentValue)

        inputEditText.inputType = InputType.TYPE_CLASS_NUMBER

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change $title")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newValue = inputEditText.text.toString()
                onSave(newValue)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getTheme(): Int {
        return R.style.MyBottomSheetDialogTheme
    }

    interface ProjectSettingsDialogListener {
        fun onSettingsSaved(projectSettingsData: ProjectSettingsData)
    }

    companion object {
        fun newInstance(project: ProjectSettingsData): ProjectSettingsFragment {
            val fragment = ProjectSettingsFragment()
            val args = Bundle()
            val serialized = Json.encodeToString(project)
            args.putSerializable("project", serialized)
            fragment.arguments = args
            return fragment
        }
    }
}
