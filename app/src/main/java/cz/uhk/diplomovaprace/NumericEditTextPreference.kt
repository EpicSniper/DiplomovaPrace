package cz.uhk.diplomovaprace

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class NumericEditTextPreference(context: Context, attrs: AttributeSet) : EditTextPreference(context, attrs) {

    init {
        setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }
}
