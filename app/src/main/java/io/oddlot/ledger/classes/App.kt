package io.oddlot.ledger.classes

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import io.oddlot.ledger.data.AppDatabase

val reqCodes = arrayOf(
    "READ_EXTERNAL_STORAGE",
    "WRITE_EXTERNAL_STORAGE",
    "CAMERA",
    "CLOSE_TAB",
    "CREATE_DOCUMENT"
)

class ReqCodes {
    companion object {
        const val READ_EXTERNAL_STORAGE = 0
        const val WRITE_EXTERNAL_STORAGE = 1
        const val CAMERA = 2
        const val CLOSE_TAB = 3
        const val CREATE_DOCUMENT = 4
    }
}

class App {
    companion object {
        @Volatile
        private var instance: App? = null
        private var database:  AppDatabase? = null
        private var queue: RequestQueue? = null
        private var prefs: SharedPreferences? = null

        fun getInstance(context: Context): App {
            return instance
                ?: App().also {
                instance = it
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return database ?: Room.databaseBuilder(context, AppDatabase::class.java, "AppDatabase")
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

        fun getRequestQueue(context: Context): RequestQueue? {
            if (queue == null) {
                queue = Volley.newRequestQueue(context)
            }
            return queue
        }
    }

//    val requestQueue: RequestQueue by lazy {
//        Volley.newRequestQueue(context.applicationContext)
//    }

//    val requestQueue: RequestQueue by lazy {
//        // applicationContext is key, it keeps you from leaking the
//        // Activity or BroadcastReceiver if someone passes one in.
//        Volley.newRequestQueue(context)
//    }

//    fun <T> addToRequestQueue(req: Request<T>) {
//        requestQueue.add(req)
//    }
}