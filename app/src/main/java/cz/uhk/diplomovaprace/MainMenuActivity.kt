package cz.uhk.diplomovaprace

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val imageButton1: ImageView = findViewById(R.id.imageButton1)
        imageButton1.setOnClickListener { view ->
            val intent = Intent(this, PianoRollActivity::class.java)
            startActivity(intent)
        }

        val imageButton2: ImageView = findViewById(R.id.imageButton2)
        imageButton2.setOnClickListener { view ->
            val intent = Intent(this, SavedFilesActivity::class.java)
            startActivity(intent)
        }

        val imageButton3: ImageView = findViewById(R.id.imageButton3)
        imageButton3.setOnClickListener { view ->
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}