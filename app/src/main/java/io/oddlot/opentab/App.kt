package io.oddlot.opentab

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.room.Room
import io.oddlot.opentab.data.AppDatabase
import io.oddlot.opentab.data.Member
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

lateinit var db: AppDatabase
var applicationFont = 0

class App : Application() {
    val TAG = this::class.java.simpleName

    override fun onCreate() {
        super.onCreate()

        val prefs = getPrefs(this)
        val isFirstRun = prefs.getBoolean(PreferenceKeys.FIRST_RUN, true)

        // Regular startup initialization
        db = appDatabase(applicationContext)
        setAppTheme()

        applicationFont = R.font.quicksand

        // additional setup if first run
        if (isFirstRun) {
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .edit()
                .putString(PreferenceKeys.THEME, PreferenceValues.FOLLOW_SYSTEM)
                .putString(PreferenceKeys.BASE_CURRENCY, "USD")
                .putBoolean(PreferenceKeys.FIRST_RUN, false)
                .apply()

            prefs.edit()
                .putString(PreferenceKeys.BASE_CURRENCY, "USD")
                .putBoolean(PreferenceKeys.FIRST_RUN, false)
                .apply()

            CoroutineScope(Dispatchers.IO).launch {
                db.memberDao().insert(Member(0, "Nameless"))
            }
        }
    }

    companion object {
        @Volatile
        private var database:  AppDatabase? = null
        private var prefs: SharedPreferences? = null

        fun appDatabase(context: Context): AppDatabase {
            return database ?: Room.databaseBuilder(context, AppDatabase::class.java, "AppDatabase")
                .addMigrations(AppDatabase.MIGRATION_2_3)
//                .fallbackToDestructiveMigration()
                .build()
                .also {
                    database = it
                }
        }

        fun getPrefs(context: Context): SharedPreferences {
            return prefs ?: context.getSharedPreferences(
                "io.oddlot.ledger.prefs", AppCompatActivity.MODE_PRIVATE)
                .also {
                    prefs = it
                }
        }

        fun getUsername(context: Context): String? {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(PreferenceKeys.USER_NAME, "null")
        }
    }

    private fun setAppTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val theme = sharedPreferences.getString(PreferenceKeys.THEME, getString(R.string.followSystemKey))

        when (theme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "followSystem" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

abstract class PreferenceKeys {
    companion object {
        const val FIRST_RUN = "firstRun"
        const val THEME = "theme"
        const val USER_NAME = "userName"
        const val BASE_CURRENCY = "baseCurrency"
    }
}

abstract class PreferenceValues {
    companion object {
        const val FOLLOW_SYSTEM = "followSystem"
    }
}

abstract class RequestCodes {
    companion object {
        const val READ_EXTERNAL_STORAGE = 0
        const val WRITE_EXTERNAL_STORAGE = 1
        const val CAMERA = 2
        const val CLOSE_TAB = 3
        const val CREATE_DOCUMENT = 4
    }
}

abstract class Theme {
    companion object {
        const val LIGHT = 0
        const val DARK = 1
        const val FOLLOW_SYSTEM = 2

        fun nameFromId(themeId: Int, context: Context? = null): String {
            return when (themeId) {
                LIGHT -> context?.getString(R.string.light) ?: "Light"
                DARK -> context?.getString(R.string.dark) ?: "Dark"
                FOLLOW_SYSTEM -> context?.getString(R.string.followSystem) ?: "Follow system"
                else -> "Invalid theme"
            }
        }
    }
}