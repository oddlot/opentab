package io.oddlot.opentab.presentation.misc

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import io.oddlot.opentab.PreferenceKey
import io.oddlot.opentab.R
import io.oddlot.opentab.db
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

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

        val userName = findPreference<EditTextPreference>(PreferenceKey.USER_NAME)?.apply {
            summary = text
            setOnPreferenceChangeListener { preference, newValue ->  userNameListener(preference, newValue.toString()) }
        }

        val baseCurrency = findPreference<ListPreference>(PreferenceKey.BASE_CURRENCY).apply {
            this?.setOnPreferenceChangeListener { preference, newValue ->  baseCurrencyListener(preference, newValue) }
        }

        val theme = findPreference<ListPreference>(PreferenceKey.THEME)?.apply {
            setOnPreferenceChangeListener { pref, newValue ->  themeListener(pref, newValue) }
        }

    }

    private fun userNameListener(preference: Preference, newValue: String): Boolean {
        if (newValue.isEmpty()) return false

        val newName = newValue.trim()

        preference.summary = newName

        CoroutineScope(IO).launch {
            db.memberDao().updateMemberName(0, newName)
        }

        return true
    }

    private fun baseCurrencyListener(preference: Preference, newValue: Any): Boolean {
        PreferenceManager.getDefaultSharedPreferences(activity?.applicationContext)
            .edit().putString(PreferenceKey.BASE_CURRENCY, newValue.toString())
            .apply()

        return true
    }

    private fun themeListener(preference: Preference, newValue: Any): Boolean {
        when (newValue) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "followSystem" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        return true
    }
}