package cz.uhk.diplomovaprace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

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