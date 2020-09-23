package io.oddlot.ledger.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import io.oddlot.ledger.App
import io.oddlot.ledger.PreferenceKeys
import io.oddlot.ledger.R
import io.oddlot.ledger.Theme
import io.oddlot.ledger.activities.prefs
import io.oddlot.ledger.utils.commatize

class SettingsFragment : PreferenceFragmentCompat() {
    private val TAG = this::class.qualifiedName
    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mContext = context
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findPreference<ListPreference>(PreferenceKeys.BASE_CURRENCY)!!.apply {
            setOnPreferenceChangeListener { preference, newValue ->
                Log.d(TAG, newValue.toString())
                prefs.edit().putString(PreferenceKeys.BASE_CURRENCY, "XAU").apply()
                return@setOnPreferenceChangeListener true
            }
        }

        findPreference<ListPreference>(PreferenceKeys.THEME)!!.apply {
            setOnPreferenceChangeListener { preference, newValue ->
                Log.d(TAG, "THEME CLICKED")
                when (newValue) {
                    App.Companion.PreferenceValue.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    getString(R.string.dark) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    getString(R.string.followSystem) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                return@setOnPreferenceChangeListener true
            }
        }
    }
}