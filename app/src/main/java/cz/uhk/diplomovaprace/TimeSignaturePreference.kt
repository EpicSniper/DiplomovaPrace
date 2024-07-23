package cz.uhk.diplomovaprace

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class TimeSignaturePreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private lateinit var timeSignatureTop: EditText
    private lateinit var timeSignatureBottom: EditText

    override fun onBindViewHolder(holder: PreferenceViewHolder) {super.onBindViewHolder(holder)

        val view = holder.itemView
        timeSignatureTop = view.findViewById(R.id.timeSignatureTop)
        timeSignatureBottom = view.findViewById(R.id.timeSignatureBottom)
    }
}