package cz.uhk.diplomovaprace.Settings

import android.content.Context
oimport android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import cz.uhk.diplomovaprace.R

class TimeSignaturePreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private lateinit var timeSignatureTop: EditText
    private lateinit var timeSignatureBottom: EditText

    init {
        // Inflate the layout here
        layoutResource = R.layout.pref_time_signature
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {super.onBindViewHolder(holder)
        super.onBindViewHolder(holder)
        val view = holder.itemView
        timeSignatureTop = view.findViewById(R.id.timeSignatureTop)
        timeSignatureBottom = view.findViewById(R.id.timeSignatureBottom)

        val numerator = sharedPreferences?.getInt("time_signature_numerator", 4)
        val denominator = sharedPreferences?.getInt("time_signature_denominator", 4)
        timeSignatureTop.setText(numerator.toString())
        timeSignatureBottom.setText(denominator.toString())

        timeSignatureTop.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                val numerator = s.toString().toIntOrNull() ?: 4 // Default to 4 if invalid
                sharedPreferences?.getInt("time_signature_denominator", 4)
                    ?.let { saveTimeSignature(numerator, it) }
            }
        })

        timeSignatureBottom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                val denominator = s.toString().toIntOrNull() ?: 4 // Default to 4 if invalid
                sharedPreferences?.let { saveTimeSignature(it.getInt("time_signature_numerator", 4), denominator) }
            }
        })
    }

    private fun saveTimeSignature(numerator: Int, denominator: Int) {
        val editor = sharedPreferences?.edit()
        editor?.putInt("time_signature_numerator", numerator)
        editor?.putInt("time_signature_denominator", denominator)
        editor?.apply()
    }

    fun getTimeSignatureTop(): String = timeSignatureTop.text.toString()
    fun getTimeSignatureBottom(): String = timeSignatureBottom.text.toString()
}