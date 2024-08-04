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
        val entries = arrayOf("Možnost 1", "Možnost 2", "Možnost 3") // Vaše hodnoty
        val entryValues = arrayOf("hodnota_1", "hodnota_2", "hodnota_3") // Odpovídající hodnoty
        listPreference?.entries = entries
        listPreference?.entryValues = entryValues
    }
}