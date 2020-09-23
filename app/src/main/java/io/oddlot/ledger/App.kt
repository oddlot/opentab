package io.oddlot.ledger

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import com.android.volley.RequestQueue
import io.oddlot.ledger.data.AppDatabase

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // First run initialization
        val prefs = getPrefs(this)
        if (prefs.getBoolean(PreferenceKeys.FIRST_RUN, true)) {
            prefs.edit()
                .putString(PreferenceKeys.BASE_CURRENCY, "USD")
                .putBoolean(PreferenceKeys.FIRST_RUN, false)
                .apply()
        }

        // ...proceed to regular initialization

    }

    companion object {
        @Volatile
        private var database:  AppDatabase? = null
        private var prefs: SharedPreferences? = null

        abstract class PreferenceValue {
            companion object {
                const val LIGHT = "light"
                const val DARK = "dark"
                const val FOLLOW_SYSTEM = "followSystem"
            }
        }

        /**
         * No longer needed since switching to PreferenceScreen
         */
//        fun setTheme(context: Context) {
//            val themeId = getPrefs(context).getInt(PreferenceKeys.THEME, Theme.LIGHT)
//
//            when (themeId) {
//                Theme.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//                Theme.DARK-> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//                Theme.FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
//            }
//        }

        fun getDatabase(context: Context): AppDatabase {
            return database ?: Room.databaseBuilder(context, AppDatabase::class.java, "AppDatabase")
//                .addMigrations(AppDatabase.MIGRATION_1_2)
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

        val bundle = Bundle.CREATOR
    }
}