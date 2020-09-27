package io.oddlot.ledger.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import io.oddlot.ledger.PreferenceKeys
import io.oddlot.ledger.R
import java.util.*

class SettingsFragment : PreferenceFragmentCompat() {
    private val TAG = this::class.java.simpleName
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

        val userNamePref = findPreference<EditTextPreference>(PreferenceKeys.USER_NAME)
        userNamePref?.setOnPreferenceChangeListener { preference, newValue ->  baseCurrencyListener(preference, newValue) }

        val baseCurrencyPref = findPreference<ListPreference>(PreferenceKeys.BASE_CURRENCY).apply {
//            this?.entries = resources.getStringArray(R.array.currencySymbols)
            this?.setOnPreferenceChangeListener { preference, newValue ->  baseCurrencyListener(preference, newValue) }
        }

        val themePref = findPreference<ListPreference>(PreferenceKeys.THEME)
        themePref?.setOnPreferenceChangeListener { pref, newValue ->  themeListener(pref, newValue) }

    }

    private fun baseCurrencyListener(preference: Preference, newValue: Any): Boolean {
        PreferenceManager.getDefaultSharedPreferences(activity?.applicationContext)
            .edit().putString(PreferenceKeys.BASE_CURRENCY, newValue.toString())
            .apply()

        return true
    }

    private fun themeListener(preference: Preference, newValue: Any): Boolean {
        when (newValue) {
            getString(R.string.light) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            getString(R.string.dark) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            getString(R.string.followSystem) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        return true
    }
}