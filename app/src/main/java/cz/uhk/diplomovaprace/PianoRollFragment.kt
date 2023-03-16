package cz.uhk.diplomovaprace

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import cz.uhk.diplomovaprace.PianoRoll.PianoRollView


/**
 * A simple [Fragment] subclass.
 * Use the [PianoRollFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PianoRollFragment : Fragment() {

    private lateinit var pianoRollView: PianoRollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_piano_roll, container, false)
        pianoRollView = view.findViewById(R.id.piano_roll_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set the data for the piano roll view
    }
}