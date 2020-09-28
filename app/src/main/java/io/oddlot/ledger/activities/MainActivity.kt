package io.oddlot.ledger.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.os.PersistableBundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import io.oddlot.ledger.TabManager
import io.oddlot.ledger.PreferenceKeys
import io.oddlot.ledger.R
import io.oddlot.ledger.utils.basicEditText
import io.oddlot.ledger.data.*
import io.oddlot.ledger.db
import io.oddlot.ledger.fragments.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import kotlin.concurrent.thread

lateinit var prefs: SharedPreferences

class MainActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var fragmentManager: FragmentManager
    private lateinit var drawerMenu: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mainFragment: MainFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = TabManager.getPrefs(applicationContext)

        // Configure toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.background = null
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Main Viewpager
        mainFragment = MainFragment()

        fragmentManager = supportFragmentManager.also {
            it.beginTransaction()
                .replace(R.id.container, mainFragment, "HOME")
                .commit()
        }

        // Nav drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close).apply {
            isDrawerSlideAnimationEnabled = false
            syncState()
        }
        drawerLayout.addDrawerListener(drawerToggle)
        drawerMenu = findViewById(R.id.nav_drawer)
        drawerMenu.setCheckedItem(R.id.nav_item_home)
        drawerMenu.bringToFront()
        drawerMenu.setNavigationItemSelectedListener { selectedItem -> drawerMenuListener(selectedItem) }

        /*
        Open Tab Fab
         */
        findViewById<FloatingActionButton>(R.id.fab).apply {
            setOnClickListener {
                // Individual Tab
                val builder = AlertDialog.Builder(context!!).apply {
                    setTitle("Create New Tab")

                    val tabNameInput = basicEditText(context).also {
                        it.requestFocus()
                        it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        it.typeface = ResourcesCompat.getFont(context, R.font.rajdhani)
                    }
                    val container = FrameLayout(context).apply {
                        addView(tabNameInput)
                    }

                    setView(container)

                    setPositiveButton("OK") { dialog, which ->
                        try {
                            // Throw exception if no name is entered
                            val inputText = tabNameInput.text
                            if (tabNameInput.text.isBlank() or (inputText.length > 25))
                                throw IllegalArgumentException("Exception")
                            else {
                                // Local
                                thread {
                                    val baseCurrency = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                                        .getString(PreferenceKeys.BASE_CURRENCY, "USD")
                                    val newTab = Tab(null, tabNameInput.text.toString(), 0.0, baseCurrency!!)
                                    db.tabDao().insertTab(newTab)

                                    runOnUiThread {
                                        fragmentManager
                                            .beginTransaction()
                                            .replace(R.id.container, MainFragment())
                                            .commit()
                                    }
                                }
                            }

                        } catch (e: Exception) {
                            when(e) {
                                is IllegalArgumentException -> {
                                    if (tabNameInput.text.isBlank())
                                        tabNameInput.error = "Name is required!"
//                                        Toast.makeText(context, "Name is required", Toast.LENGTH_LONG).show()
                                    else
                                        Toast.makeText(
                                            context,
                                            "Tab name cannot be more than 25 characters long",
                                            Toast.LENGTH_LONG
                                        ).show()
                                }
                                is SQLiteConstraintException -> {
                                    Log.d(TAG, "SQLiteConstraintException")

                                    Toast.makeText(
                                        context,
                                        "A tab with that name already exists",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                else -> throw e
                            }
                        }
                    }
                    setNegativeButton("Cancel") { dialog, which ->
                        /**
                         * Hide soft input by adding below to Activity in Manifest
                         * android:windowSoftInputMode="stateAlwaysHidden"
                         */
                    }
                }
                builder.show()

//                var groupTabDialog = AlertDialog.Builder(context!!).apply {
//                    val tabNameInput = basicEditText(
//                        context
//                    ).also { it.requestFocus() }
//                    val container = FrameLayout(context).apply {
//                        addView(tabNameInput)
//                    }
//                    setView(container)
//                    setTitle("Create a Group Tab")
//                    setPositiveButton("OK") { dialog, which ->
//                        try {
//                            // Throw exception if no name is entered
//                            val inputText = tabNameInput.text
//                            if (tabNameInput.text.isBlank() or (inputText.length > 32))
//                                throw IllegalArgumentException("Exception")
//                            else {
//                                val tabName = tabNameInput.text.toString()
//
//                                CoroutineScope(IO).launch {
//                                    val groupTabId = db.tabDao().insert(
//                                        Tab(null, tabName, isGroup=true)
//                                    )
//
//                                    val newMS = Membership(null, groupTabId.toInt(), 1)
//                                    db.membershipDao().insert(newMS)
//                                }
//                            }
//
//                        } catch (e: IllegalArgumentException) {
//                            if (tabNameInput.text.isBlank())
//                                Toast.makeText(
//                                    context,
//                                    "Name is required",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                            else
//                                Toast.makeText(
//                                    context,
//                                    "Name cannot be greater than 32 characters",
//                                    Toast.LENGTH_LONG
//                                ).show()
//                        }
//                    }
//                    setNegativeButton("Cancel") { dialog, which ->
//                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                        imm.hideSoftInputFromWindow(tabNameInput.windowToken, 0)
//                        dialog.cancel()
//                    }
//                }
//                groupTabDialog.show()

                // Show soft keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
            }

            setOnLongClickListener {
                var groupTabDialog = AlertDialog.Builder(context!!).apply {
                    val tabNameInput =
                        basicEditText(context)
                    val container = FrameLayout(context).apply {
                        addView(tabNameInput)
                    }
                    setView(container)
                    setTitle("Create a Group Tab")
                    setPositiveButton("OK") { dialog, which ->
                        try {
                            // Throw exception if no name is entered
                            val inputText = tabNameInput.text
                            if (tabNameInput.text.isBlank() or (inputText.length > 15))
                                throw IllegalArgumentException("Exception")
                            else {
                                val tabName = tabNameInput.text.toString()

                                CoroutineScope(Dispatchers.IO).launch {
                                    val groupTabId = db.tabDao().insertTab(
                                        Tab(null, tabName)
                                    )

                                    val newMS = Membership(null, groupTabId.toInt(), 1)
                                    db.membershipDao().insert(newMS)
                                }
                            }

                        } catch (e: IllegalArgumentException) {
                            if (tabNameInput.text.isBlank())
                                Toast.makeText(
                                    context,
                                    "Name is required",
                                    Toast.LENGTH_LONG
                                ).show()
                            else
                                Toast.makeText(
                                    context,
                                    "Tab name must be 15 characters or less",
                                    Toast.LENGTH_LONG
                                ).show()
                        }
                    }
                    setNegativeButton("Cancel") { dialog, which ->
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(tabNameInput.windowToken, 0)
                        dialog.cancel()
                    }
                }
                groupTabDialog.show()

                true
            }
        }

        checkIfFirstRun()
        setGreeting()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_overflow, menu)

        return true

    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)

        outState.putInt("PAGER_POSITION", 1)
    }

    override fun onResume() {
        Log.d(TAG, "Resuming activity")
        super.onResume()

        setGreeting()
    }

    override fun onRestart() {
        super.onRestart()

        intent.extras?.getBoolean("RESTART_ACTIVITY")?.let {
            // Refresh main fragment
            fragmentManager.beginTransaction()
                .remove(mainFragment)
                .replace(R.id.container, MainFragment(), "HOME")
                .commitAllowingStateLoss()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        return when (item.itemId) {
            R.id.menu_help -> {
                startActivity(Intent(this, HelpActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        val nav = findViewById<NavigationView>(R.id.nav_drawer)
        val settingsFragment = fragmentManager.findFragmentByTag("SETTINGS")

        // Re-check 'Home' menu item if going back from Settings fragment
        if (settingsFragment?.isVisible == true) {
            val homeMenuItem = nav.menu.getItem(0)
            nav.setCheckedItem(homeMenuItem)
        }

        when {
            // Close left navigation menu
            drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawer(GravityCompat.START)
            // Close right navigation menu
            drawerLayout.isDrawerOpen(GravityCompat.END) -> drawerLayout.closeDrawer(GravityCompat.END)
            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun checkIfFirstRun() {
        val username = prefs.getString(PreferenceKeys.USER_NAME, null)
        if (username == null) {
            val usernameInput = basicEditText(this).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES }

            val container = FrameLayout(this).apply {
                addView(usernameInput)
            }

            val builder = AlertDialog.Builder(this).apply {
                setView(container)
                setTitle("Hi! What's your name?")
                setCancelable(false)
                setPositiveButton("OK") { dialog, which ->
                    // Caught in event listener below
                }

                // Unreachable if setCancelable = false
                setOnCancelListener {
                    moveTaskToBack(true)
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(1)
                }
            }

            /* Catch Positive button event */
            builder.create().apply {
                show()
                getButton(-1).setOnClickListener {
                    if (usernameInput.text.isEmpty()) {
                        usernameInput.error = "I'm sorry, didn't catch your name!"
                    }
                    else {
                        val newName = usernameInput.text.toString()

                        CoroutineScope(Dispatchers.IO).launch {
                            db.memberDao().updateMemberName(0, newName)
                        }

                        prefs.edit().putString(PreferenceKeys.USER_NAME, newName).apply()
                        PreferenceManager.getDefaultSharedPreferences(applicationContext)
                            .edit()
                            .putString(PreferenceKeys.USER_NAME, newName)
                            .apply()

                        setGreeting()

                        this.dismiss()
                    }
                }
            }
        }
    }

    private fun setGreeting() {
        val hv = drawerMenu.getHeaderView(0).findViewById<TextView>(R.id.navHeaderTextPrimary)
        val name = PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceKeys.USER_NAME, "No name")
        val c = Calendar.getInstance()

        hv.text = "$name"

        when (c.get(Calendar.HOUR_OF_DAY)) {
            in 0 until 12 -> hv.text = "Good morning, $name"
            in 12 until 18 -> hv.text = "Good afternoon, $name"
            else -> hv.text = "Good evening, $name"
        }
    }

    private fun drawerMenuListener(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.nav_item_home -> {
                // Clear back stack on Home (Main) fragment
                fragmentManager.backStackEntryCount.also {
                    if (it > 0) {
                        for (i in 1..it) fragmentManager.popBackStack()
                    }
                }

                // Replace Home fragment if not visible
                if (!mainFragment.isVisible) {
                    fragmentManager
                        .beginTransaction()
                        .replace(R.id.container, mainFragment)
                        .commit()
                }

                menuItem.isChecked = true
                drawerLayout.closeDrawer(GravityCompat.START)

                true
            }
            R.id.nav_item_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java), null)

//                drawerMenu.menu.getItem(0).isChecked = true

                true
            }
//            R.id.nav_item_help -> {
//                fragmentManager
//                    .beginTransaction()
//                    .replace(R.id.container, HelpFragment(), "HELP")
//                    .addToBackStack(null)
//                    .commit()
//                menuItem.isChecked = true
//                drawerLayout.closeDrawer(GravityCompat.START)
//                supportActionBar?.setBackgroundDrawable(null)
//
//                true
//            }
            R.id.nav_item_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> false
        }
    }
}