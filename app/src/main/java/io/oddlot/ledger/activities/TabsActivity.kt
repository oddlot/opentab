package io.oddlot.ledger.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.RequestQueue
import io.oddlot.ledger.App
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.TabsAdapter
import io.oddlot.ledger.view_models.TabsViewModel
import io.oddlot.ledger.data.Member
import io.oddlot.ledger.data.Tab
import kotlinx.android.synthetic.main.fragment_tabs.*
import java.util.*
import kotlin.concurrent.thread

class TabsActivity : AppCompatActivity() {
    private val TAG = "TABS_ACTIVITY"
    private lateinit var mPrefs: SharedPreferences
    private lateinit var viewModel: TabsViewModel
    private var mTabs: MutableList<Tab> = mutableListOf()
    private lateinit var welcomeMessageView: TextView
    private lateinit var mQueue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_tabs)

        mPrefs = getSharedPreferences("io.oddlot.ledger.prefs", MODE_PRIVATE)
        mQueue = App.getRequestQueue(applicationContext)!!


        // Configure tabs RecyclerView
        tabsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load ViewModel and LiveData objects
        viewModel = ViewModelProviders.of(this).get(TabsViewModel::class.java)
        viewModel.getAll().observe(this, Observer {
            // Update RecyclerView on change
            tabsRecyclerView.adapter = TabsAdapter(it).apply {
                notifyDataSetChanged()
            }
        })

        val c = Calendar.getInstance()
        val timeOfDay = c.get(Calendar.HOUR_OF_DAY)

        // Populate Views
        thread {
            Looper.prepare()

            runOnUiThread {
                checkForUser()

                val username = mPrefs.getString("USERNAME", "guest")
                val nameView = SpannableStringBuilder(username)
                nameView.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, username!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                when (timeOfDay) {
                    in 0 until 12 -> welcomeMessageView.text = "Good morning, $nameView!"
                    in 12 until 18 -> welcomeMessageView.text = "Good afternoon, $nameView!"
                    else -> welcomeMessageView.text = "Good evening, $nameView!"
                }
            }
        }
//        loadTabsView()

        createTabFab.setOnClickListener {
            val tabNameInput = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT
                inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                showSoftInputOnFocus = true
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 50
                    leftMargin = 50
                    rightMargin = 50
                }
            }

            val container = FrameLayout(this)
//            container.addView(TextView(this).apply{
//                this.text = ""
//            })
            container.addView(tabNameInput)

            val dialog = AlertDialog.Builder(this).apply {
                setView(container)
                setTitle("Create a New Tab")

                setPositiveButton("OK") { dialog, which ->
                    try {
                        // Throw exception if no name is entered
                        var inputText = tabNameInput.text
                        if (tabNameInput.text.isBlank() or (inputText.length > 15))
                            throw IllegalArgumentException("Exception")
                        else {
                            // Local
                            thread {
                                val newTab = Tab(null, tabNameInput.text.toString(), 0.0)
                                db.tabDao().insert(newTab)

                                val intent = Intent(it.context, TabsActivity::class.java).apply {
//                                        putExtra("TAB_PARCELABLE", TabParcelable(newTab.id!!, newTab.name!!))
                                }
                                startActivity(intent)
                                finish()
                            }
                        }

                    } catch (e: IllegalArgumentException) {
                        if (tabNameInput.text.isBlank())
                            Toast.makeText(context, "Name is required", Toast.LENGTH_LONG).show()
                        else
                            Toast.makeText(context, "Tab name must be 15 characters or less", Toast.LENGTH_LONG).show()
                    }
                }

                setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
            }
            dialog.show()
        }
    }

//    override fun onResume() {
//        super.onResume()
//        Log.d(TAG, "Resuming")
//
//        loadTabsView()
//    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "Restarting")

//        loadTabsView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.overflow_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
//            R.id.menu_settings -> {
//                startActivity(Intent(this, SettingsActivity::class.java))
//                true
//            }
            R.id.menu_help -> {
//                startActivity(Intent(this, HelpActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // In-Class Functions
    private fun checkForUser() {
        var username = mPrefs.getString("USERNAME", null)

        if (username == null) {
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 50
                leftMargin = 50
                rightMargin = 50
            }
            val usernameInput = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_TEXT
                inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                showSoftInputOnFocus = true
                layoutParams = params
            }

            val container = FrameLayout(this)
            container.addView(usernameInput)

            val builder = AlertDialog.Builder(this).apply {
                // Config dialog box
                setView(container)
                setTitle("Hi! What's your name?")
                setCancelable(false)

                // Config click listeners
                setPositiveButton("OK") { dialog, which -> }

                // Unreachable if setCancelable = false
                setOnCancelListener {
                    moveTaskToBack(true)
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(1)
                }
            }

            // Show dialog and configure positive button listener
            builder.create().apply {
                show()
                getButton(-1).setOnClickListener {
                    if (usernameInput.text.isEmpty()) {
                        usernameInput.error = "I'm sorry, didn't catch your name!"
                    }
                    else {
                        val firstMember = Member(null, usernameInput.text.toString())
                        mPrefs.edit().putString("USERNAME", firstMember.name).apply()
                        welcomeMessageView.text = "Good Morning, ${usernameInput.text}!"

                        this.dismiss()
                    }
                }
            }
        }
    }
}