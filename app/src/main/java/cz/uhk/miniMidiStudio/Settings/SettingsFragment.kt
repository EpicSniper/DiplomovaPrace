package cz.uhk.miniMidiStudio.Settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import cz.uhk.miniMidiStudio.R

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.preference_container, PreferenceFragment())
                .commit()
        }
    }
}