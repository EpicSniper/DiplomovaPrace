package cz.uhk.diplomovaprace

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import cz.uhk.diplomovaprace.PianoRoll.PianoRollView
import cz.uhk.diplomovaprace.databinding.ActivityMainBinding

class PianoRollActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var pianoRollView: PianoRollView

    private val permissionLauncherSingle = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("Yes")
        } else {
            println("Nope")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //val navController = findNavController(R.id.nav_host_fragment_content_main)
        //appBarConfiguration = AppBarConfiguration(navController.graph)

        // TODO: another permissions
        pianoRollView = findViewById(R.id.piano_roll_view)
        permissionLauncherSingle.launch(android.Manifest.permission.RECORD_AUDIO)
        /* tlacitko
        binding.fab.setOnClickListener { view ->
            pianoRollView.changeEditingMode()
        }
        */
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}