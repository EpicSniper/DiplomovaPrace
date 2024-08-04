package cz.uhk.diplomovaprace.Settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.uhk.diplomovaprace.R

class SettingsProjectDialogFragment : BottomSheetDialogFragment()  {

    private var listener: SettingsProjectDialogListener? = null
    private var projectSettingsData: ProjectSettingsData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_project_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        saveButton.setOnClickListener {
            // 1. Gather the settings data from the dialog's UI elements
            val projectSettingsData = ProjectSettingsData(
                algorithmType = sharedPreferences.getString("algorithm_type", "hodnota_1"),
                pitchOfA1 = sharedPreferences.getString("nota_a_frekvence", "440")?.toInt(),
                bpm = sharedPreferences.getString("bpm", "120")?.toInt(),
                timeSignatureNumerator = sharedPreferences.getInt("time_signature_numerator", 4),
                timeSignatureDenominator = sharedPreferences.getInt("time_signature_denominator", 4)
            )

            // 2. Trigger the callback to pass the data to the PianoRollFragment
            listener?.onSettingsSaved(projectSettingsData)

            // 3. Optionally, dismiss the dialog
            dismiss()
        }
    }

    fun setProjectSettings(settings: ProjectSettingsData) {
        // Store the project settings in a variable
        this.projectSettingsData = settings
    }

    fun setListener(listener: SettingsProjectDialogListener) {
        this.listener = listener
    }

    override fun getTheme(): Int {
        return R.style.MyBottomSheetDialogTheme
    }

    interface SettingsProjectDialogListener {
        fun onSettingsSaved(projectSettingsData: ProjectSettingsData)
    }
}