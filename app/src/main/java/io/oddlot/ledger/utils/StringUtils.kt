package io.oddlot.ledger.utils

import android.app.Activity
import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Filter
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import io.oddlot.ledger.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.round

class StringUtils {
    companion object {
        /**
         * Convert long to date string
         * Example: yyyy/MM/dd = 1988/10/14
         */
        @JvmStatic
        fun dateStringFromMillis(millis: Long, datePattern: String = "yyyy-MM-dd"): String {
//            val dtf = DateTimeFormatter.ofPattern(datePattern)
//            return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(dtf)
            val sdf = SimpleDateFormat(datePattern)
            return sdf.format(Date(millis))
        }

        /**
         * Convert date string to long
         */
        @JvmStatic
        fun millisFromDateString(dateString: String, datePattern: String = "yyyy-MM-dd"): Long {
            val sdf = SimpleDateFormat(datePattern)

            return sdf.parse(dateString)!!.time
        }
    }
}

fun Long.toDateString(pattern: String = "yyyy-MM-dd"): String {
    val dtf = DateTimeFormatter.ofPattern(pattern)

    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(dtf)
}

/**
 * Double rounder
 */
fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) {
        multiplier *= 10
    }
    return round(this * multiplier) / multiplier
}

fun <T: Any> T.commatize(): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
}

fun promptDialog(message: String?, callback: (() -> Unit)?) {
    callback?.invoke()
}

//fun basicDialog(context: Context, title: String, pos: String? = null, neg: String? = null): AlertDialog {
//    val builder = AlertDialog.Builder(context).apply {
//        val container = FrameLayout(context)
//        // Config dialog box
//        setView(container)
//        setTitle(title)
//
//        // Config click listeners
//        setPositiveButton(pos) { dialog, which -> }
//        setNegativeButton(neg) { dialog, which -> }
//    }
//
//    // Return dialog
//    return builder.create()
//}

class BasicDialog(context: Context, title: String, pos: String? = null, neg: String? = null): AlertDialog(context) {
    init {
//        this.setView(FrameLayout(context))
        this.setTitle(title)

        // Config click listeners
        pos?.let {
//            setPositiveButton(pos) { dialog, which -> }
//            setNegativeButton(pos) { dialog, which -> }
            setButton(BUTTON_POSITIVE, pos) { dialog, which -> }
            setButton(BUTTON_NEGATIVE, neg) { dialog, which -> }
        }
    }
}

fun basicEditText(context: Context): EditText {
    val input = EditText(context).apply {
        inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        showSoftInputOnFocus = true

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                resources.getDimensionPixelSize(R.dimen.DIALOG_INPUT_MARGIN),
                50,
                resources.getDimensionPixelSize(R.dimen.DIALOG_INPUT_MARGIN),
                15
            )
        }

        layoutParams = lp
    }

    return input
}

fun View.hideKeyboard(activity: Activity) {
    val view = activity.currentFocus

    view?.let {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}