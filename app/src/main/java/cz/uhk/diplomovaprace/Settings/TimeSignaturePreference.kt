package cz.uhk.diplomovaprace.Settings

import android.content.Context
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
    }

    fun getTimeSignatureTop(): String = timeSignatureTop.text.toString()
    fun getTimeSignatureBottom(): String = timeSignatureBottom.text.toString()
}