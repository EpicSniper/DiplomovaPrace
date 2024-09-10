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

        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            if (android.os.Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)

        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permissions[2]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, RECORD_AUDIO_PERMISSION_REQUEST_CODE)
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
