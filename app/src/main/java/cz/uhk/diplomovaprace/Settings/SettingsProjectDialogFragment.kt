package cz.uhk.diplomovaprace.Settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.uhk.diplomovaprace.R

class SettingsProjectDialogFragment : BottomSheetDialogFragment()  {

    private var listener: SettingsProjectDialogListener? = null

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
        val sharedPreferences = requireContext().getSharedPreferences("your_preferences_name", Context.MODE_PRIVATE)
        saveButton.setOnClickListener {
            // 1. Gather the settings data from the dialog's UI elements
            val myPreferenceValue = sharedPreferences.getString("nota_a_frekvence", "440")
            val settingsData = 5

            // 2. Trigger the callback to pass the data to the PianoRollFragment
            listener?.onSettingsSaved(settingsData)

            // 3. Optionally, dismiss the dialog
            dismiss()
        }
    }

    fun setListener(listener: SettingsProjectDialogListener) {
        this.listener = listener
    }

    override fun getTheme(): Int {
        return R.style.MyBottomSheetDialogTheme
    }

    interface SettingsProjectDialogListener {
        fun onSettingsSaved(settingsData: Any)
    }
}