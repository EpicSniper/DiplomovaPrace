package cz.uhk.diplomovaprace

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.view.View
import androidx.compose.ui.text.font.FontVariation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.uhk.diplomovaprace.Settings.SettingsBottomSheetDialogFragment
import cz.uhk.diplomovaprace.Settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE= 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Zkontrolujte, zda již máte oprávnění
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Pokud nemáte oprávnění, vyžádejte si ho
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_REQUEST_CODE)} else {
            // Pokud již máte oprávnění, můžete začít používat mikrofon
            // ...
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Oprávnění bylo uděleno, můžete začít používat mikrofon
                // ...
            } else {
                // Oprávnění bylo odepřeno, informujte uživatele
                // ...
            }
        }
    }
}