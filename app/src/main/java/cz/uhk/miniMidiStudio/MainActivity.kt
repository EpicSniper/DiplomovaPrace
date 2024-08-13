package cz.uhk.miniMidiStudio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cz.uhk.miniMidiStudio.R

class MainActivity : AppCompatActivity() {

    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 100
    private val REQUEST_WRITE_STORAGE_PERMISSION = 101
    private val REQUEST_READ_STORAGE_PERMISSION = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_REQUEST_CODE)
        }

// Vyžádání oprávnění pro zápis do úložiště
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE_PERMISSION)
        }

// Vyžádání oprávnění pro čtení z úložiště (v závislosti na verzi SDK)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), REQUEST_READ_STORAGE_PERMISSION)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_STORAGE_PERMISSION)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_READ_STORAGE_PERMISSION) {
            if (resultCode == RESULT_OK) {
                // Oprávnění pro zápis do úložiště bylo uděleno

            } else {
                // Oprávnění bylo odepřeno
                Toast.makeText(this, "Permission to write storage denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RECORD_AUDIO_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Oprávnění k záznamu zvuku bylo uděleno
                } else {
                    // Oprávnění k záznamu zvuku bylo odepřeno
                    Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_WRITE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Oprávnění k zápisu do úložiště bylo uděleno

                } else {
                    // Oprávnění k zápisu do úložiště bylo odepřeno
                    Toast.makeText(this, "Permission to write storage denied", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_READ_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Oprávnění k čtení z úložiště bylo uděleno

                } else {
                    // Oprávnění k čtení z úložiště bylo odepřeno
                    Toast.makeText(this, "Permission to read storage denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
