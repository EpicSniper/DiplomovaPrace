package cz.uhk.miniMidiStudio.Settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import cz.uhk.miniMidiStudio.R
import cz.uhk.miniMidiStudio.PianoRoll.PitchDetectionMethod

class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val listPreference = findPreference<ListPreference>("algorithm_type")
        val entries = arrayOf(PitchDetectionMethod.AUTOCORRELATION.method, PitchDetectionMethod.HPS.method)
        val entryValues = arrayOf(PitchDetectionMethod.AUTOCORRELATION.name, PitchDetectionMethod.HPS.name)
        listPreference?.entries = entries
        listPreference?.entryValues = entryValues
    }
}