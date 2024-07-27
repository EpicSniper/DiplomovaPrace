package cz.uhk.diplomovaprace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cz.uhk.diplomovaprace.Settings.SettingsBottomSheetDialogFragment

class MainMenuFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageButton1: ImageView = view.findViewById(R.id.imageButton1)
        imageButton1.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_newRecording)
        }

        val imageButton2: ImageView = view.findViewById(R.id.imageButton2)
        imageButton2.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_savedFilesFragment)
        }

        val imageButton3: ImageView = view.findViewById(R.id.imageButton3)
        imageButton3.setOnClickListener {
            val bottomSheetDialog = SettingsBottomSheetDialogFragment()
            bottomSheetDialog.show(parentFragmentManager, "SettingsBottomSheet")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_menu, container, false)
    }
}