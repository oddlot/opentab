package io.oddlot.ledger.fragments

import android.app.ActionBar
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import io.oddlot.ledger.R
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.activities.prefs
import io.oddlot.ledger.utils.UsernameFilter
import io.oddlot.ledger.utils.basicEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val nameView = view.findViewById<TextView>(R.id.name).also { nv ->
            nv.text = prefs.getString("USERNAME", "null")
        }

        val editIcon = view.findViewById<ImageView>(R.id.editNameIcon)

        editIcon.setOnClickListener {
            val input = basicEditText(context!!).apply {
                text = SpannableStringBuilder(prefs.getString("USERNAME", ""))
                typeface = ResourcesCompat.getFont(context, R.font.app_font)
                setSelection(text.length)
                filters = arrayOf(UsernameFilter())
            }
//            val input = EditText(context).apply {
////                background = null
//                inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
//                showSoftInputOnFocus = true
//                gravity = Gravity.CENTER
//                text = SpannableStringBuilder(prefs.getString("USERNAME", ""))
//                setSelection(text.length)
//
//                val lp = FrameLayout.LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT
//                )
//
//                lp.setMargins(60, 50, 60, 0)
//
//                layoutParams = lp
//            }

            val builder = AlertDialog.Builder(context!!).apply {
                setTitle("Change Name")
                setView(
                    FrameLayout(context).apply {
                        addView(input)
//                        val lp = FrameLayout.LayoutParams(
//                            ViewGroup.LayoutParams.WRAP_CONTENT,
//                            ViewGroup.LayoutParams.WRAP_CONTENT
//                        ).apply {
//                            setMargins(0, 0, 0, 20)
//                        }
//
//                        layoutParams = lp
                    }
                )

                setPositiveButton("OK") { dialog, which ->
                    val newName = input.text.toString()

                    // Update name of super Member
                    CoroutineScope(Dispatchers.IO).launch {
                        db.memberDao().updateMemberName(1, newName)
                    }

                    // Update name in shared preferences
                    prefs.edit()
                        .putString("USERNAME", newName)
                        .apply()

                    // Update name in layout
                    nameView.text = newName

                    // Update name in nav drawer
                    activity!!.findViewById<NavigationView>(R.id.nav_drawer)
                        .getHeaderView(0).findViewById<TextView>(R.id.navHeaderTextPrimary).apply {
                            val c = Calendar.getInstance()

                            when (c.get(Calendar.HOUR_OF_DAY)) {
                                in 0 until 12 -> text = "Good morning, $$newName"
                                in 12 until 18 -> text = "Good afternoon, $$newName"
                                else -> text = "Good evening, $$newName"
                            }
                        }

                    // Toast new name
                    Toast.makeText(context, "$newName", Toast.LENGTH_SHORT).show()
                }
                setNegativeButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }
            }
            builder.show()
        }
        return view
    }

}