package io.oddlot.opentab

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import androidx.room.Room
import io.oddlot.opentab.data.AppDatabase
import io.oddlot.opentab.data.Member
import io.oddlot.opentab.data.Tab
import io.oddlot.opentab.data.Transaction
import io.oddlot.opentab.utils.StringUtils
import io.oddlot.opentab.utils.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

lateinit var db: AppDatabase
var applicationFont = 0

class App : Application() {
    val TAG = this::class.java.simpleName

    override fun onCreate() {
        super.onCreate()

        val prefs = getPrefs(this)
        val isFirstRun = prefs.getBoolean(PreferenceKey.FIRST_RUN, true)

        // Regular startup initialization
        db = appDatabase(applicationContext)
        setAppTheme()

        applicationFont = R.font.quicksand

        // additional setup if first run
        if (isFirstRun) {
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .edit()
                .putString(PreferenceKey.THEME, PreferenceValue.FOLLOW_SYSTEM)
                .putString(PreferenceKey.BASE_CURRENCY, "USD")
                .putBoolean(PreferenceKey.FIRST_RUN, false)
                .apply()

            prefs.edit()
                .putString(PreferenceKey.BASE_CURRENCY, "USD")
                .putBoolean(PreferenceKey.FIRST_RUN, false)
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
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .addMigrations(AppDatabase.MIGRATION_2_3)
                .addMigrations(AppDatabase.MIGRATION_3_4)
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
            return PreferenceManager.getDefaultSharedPreferences(context).getString(PreferenceKey.USER_NAME, "null")
        }

        fun restoreTransactionsFromCsv(inputStream: InputStream) {
            val reader = inputStream.reader()
            val lines = reader.readLines()

            for (i in 1 until lines.size) { // Start reading from line 2

                val rowItem = lines[i]
                    .split(",")

                val tabName =  rowItem[1]

                val transactionTab = db.tabDao().getTabByName(tabName)
                var tabId: Long = 0

                if (transactionTab == null) {
                    tabId = db.tabDao().insertTab(Tab(null, tabName))
                }

                val txn = Transaction(
                    null,
                    tabId = tabId.toInt(),
                    amount = rowItem[2].toDouble(),
                    description = rowItem[1].removeSurrounding("\""),
                    date = StringUtils.millisFromDateString(rowItem[0])
                )
                db.transactionDao().insert(txn)
            }

            inputStream.close()
        }
    }

    private fun setAppTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val theme = sharedPreferences.getString(PreferenceKey.THEME, getString(R.string.followSystemKey))

        when (theme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "followSystem" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

abstract class PreferenceKey {
    companion object {
        const val FIRST_RUN = "firstRun"
        const val THEME = "theme"
        const val USER_NAME = "userName"
        const val BASE_CURRENCY = "baseCurrency"
    }
}

abstract class PreferenceValue {
    companion object {
        const val FOLLOW_SYSTEM = "followSystem"
    }
}

abstract class RequestCode {
    companion object {
        const val READ_EXTERNAL_STORAGE = 0
        const val WRITE_EXTERNAL_STORAGE = 1
        const val CAMERA = 2
        const val CHOOSE_IMAGE = 3
        const val CLOSE_TAB = 4
        const val CREATE_DOCUMENT = 5
    }
}

abstract class ExtraKey {
    companion object {
        val NEW_TASK_ON_BACK = "NEW_TASK_ON_BACK"
        val TAB_PARCELABLE = "TAB_PARCELABLE"
    }
}

enum class TransactionType {
    DEBT, PAYMENT, TRANSFER,
}