package io.oddlot.ledger.fragments

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import io.oddlot.ledger.PreferenceKeys
import io.oddlot.ledger.R
import io.oddlot.ledger.Theme
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.activities.prefs
import io.oddlot.ledger.utils.UsernameFilter
import io.oddlot.ledger.utils.basicEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SettingsFragmentOld : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val nameView = view.findViewById<TextView>(R.id.name).also { nv ->
            nv.text = prefs.getString(PreferenceKeys.USER_NAME, "NO_NAME")
        }

        // Username Clickable
        view.findViewById<LinearLayoutCompat>(R.id.usernameClickable).setOnClickListener {
            val input = basicEditText(requireContext()).apply {
                hint = "Your Name"
                text = SpannableStringBuilder(prefs.getString(PreferenceKeys.USER_NAME, "NO_NAME"))
                typeface = ResourcesCompat.getFont(context, R.font.app_font)
                setSelection(text.length)
                filters = arrayOf(UsernameFilter())
            }
            val builder = AlertDialog.Builder(context!!).apply {
                setTitle("Your Name")
                setView(
                    FrameLayout(context).apply {
                        addView(input)
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
                        .putString(PreferenceKeys.USER_NAME, newName)
                        .apply()

                    // Update name in layout
                    nameView.text = newName

                    // Update name in nav drawer
                    activity!!.findViewById<NavigationView>(R.id.nav_drawer)
                        .getHeaderView(0).findViewById<TextView>(R.id.navHeaderTextPrimary).apply {
                            val c = Calendar.getInstance()

                            when (c.get(Calendar.HOUR_OF_DAY)) {
                                in 0 until 12 -> text = "Good morning, $newName"
                                in 12 until 18 -> text = "Good afternoon, $newName"
                                else -> text = "Good evening, $newName"
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

        // Theme Clickable
        view.findViewById<LinearLayoutCompat>(R.id.themeClickable).apply {
            val themeId = prefs.getInt(PreferenceKeys.THEME, Theme.DARK)
            val tvTheme = findViewById<TextView>(R.id.tvTheme).apply {
                text = Theme.nameFromId(themeId)
            }

            setOnClickListener {
                val rg = RadioGroup(view.context).apply {
                    val lightBtn = themeRadioButton(Theme.LIGHT)
                    val darkBtn = themeRadioButton(Theme.DARK)
                    val autoBtn = themeRadioButton(Theme.FOLLOW_SYSTEM)

                    addView(lightBtn)
                    addView(darkBtn)
                    addView(autoBtn)

                    when (prefs.getInt(PreferenceKeys.THEME, Theme.LIGHT)) {
                        Theme.LIGHT -> check(lightBtn.id)
                        Theme.DARK -> check(darkBtn.id)
                        Theme.FOLLOW_SYSTEM -> check(autoBtn.id)
                    }

                    setOnCheckedChangeListener { rg, checkedId ->
                        prefs.edit().putInt(PreferenceKeys.THEME, checkedId).apply()
//                            .also {
//                                parentFragmentManager.beginTransaction().replace(R.id.container, SettingsFragment())
//                            }
                        tvTheme.text = Theme.nameFromId(checkedId)
                    }
                }

                val builder = AlertDialog.Builder(view.context).apply {
                    setTitle("Theme")
                    setView(rg)
                }.show()
            }
        }

        return view
    }

    private fun themeRadioButton(themeId: Int): RadioButton {
        /*
        - Set up Radio Button
        - Set layout params
        - Set on checked change listener
         */
        val themeString = when (themeId) {
            Theme.LIGHT -> getString(R.string.light)
            Theme.DARK -> getString(R.string.dark)
            Theme.FOLLOW_SYSTEM -> getString(R.string.followSystem)
            else -> "null"
        }

        return RadioButton(this.context).apply {
            id = themeId
            text = themeString
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    resources.getDimensionPixelSize(R.dimen.DIALOG_INPUT_MARGIN),
                    50,
                    resources.getDimensionPixelSize(R.dimen.DIALOG_INPUT_MARGIN),
                    25
                )
            }
        }
    }
}