package cz.uhk.miniMidiStudio.Settings

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import cz.uhk.miniMidiStudio.PianoRoll.PitchDetectionMethod
import cz.uhk.miniMidiStudio.Project.Project
import cz.uhk.miniMidiStudio.Project.ProjectViewModel
import cz.uhk.miniMidiStudio.R

class NewProjectSettingsFragment : Fragment(R.layout.fragment_new_project_settings) {

    val viewModel: ProjectViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.preference_container, PreferenceFragment())
                .commitNow()
        }

        val preferenceFragment =
            childFragmentManager.findFragmentById(R.id.preference_container) as? PreferenceFragment

        view.findViewById<Button>(R.id.button3).setOnClickListener {
            val algorithmType =
                preferenceFragment?.findPreference<ListPreference>("algorithm_type")?.value
            val pitchOfA1 =
                preferenceFragment?.findPreference<EditTextPreference>("nota_a_frekvence")?.text?.toIntOrNull()
            val bpm =
                preferenceFragment?.findPreference<EditTextPreference>("bpm")?.text?.toIntOrNull()
            val timeSignaturePreference =
                preferenceFragment?.findPreference<TimeSignaturePreference>("time_signature_key")
            val timeSignatureTop = timeSignaturePreference?.getTimeSignatureTop()?.toInt()
            val timeSignatureBottom = timeSignaturePreference?.getTimeSignatureBottom()?.toInt()

            var newProject = Project()
            newProject.setName("New Project")
            newProject.setTimeSignatureLower(timeSignatureBottom ?: 4)
            newProject.setTimeSignatureUpper(timeSignatureTop ?: 4)
            newProject.setTempo(bpm ?: 120)
            newProject.setPitchOfA1(pitchOfA1 ?: 440)
            newProject.setAlgorithmType(algorithmType ?: PitchDetectionMethod.AUTOCORRELATION.name)
            viewModel.selectProject(newProject)

            findNavController().navigate(R.id.action_newProjectSettingsFragment_to_pianoRollFragment)
        }
    }
}