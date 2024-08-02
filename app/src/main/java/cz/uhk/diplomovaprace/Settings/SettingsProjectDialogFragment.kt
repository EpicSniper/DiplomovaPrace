package cz.uhk.diplomovaprace.Settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.uhk.diplomovaprace.R

class SettingsProjectDialogFragment : BottomSheetDialogFragment()  {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_project_settings, container, false)
    }

    override fun getTheme(): Int {
        return R.style.MyBottomSheetDialogTheme
    }
}