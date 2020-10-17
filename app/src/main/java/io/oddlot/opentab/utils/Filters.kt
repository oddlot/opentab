package io.oddlot.opentab.utils


import android.text.InputFilter
import android.text.Spanned

class UsernameFilter : InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence {
        return if (source!!.contains(" ")) {
            ""
        } else {
            source
        }
    }

}