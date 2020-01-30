package io.oddlot.ledger.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.SharedPreferences
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.navigation.NavigationView
import io.oddlot.ledger.classes.App
import io.oddlot.ledger.R
import io.oddlot.ledger.classes.basicEditText
import io.oddlot.ledger.data.*
import io.oddlot.ledger.fragments.DashboardFragment
import io.oddlot.ledger.fragments.SettingsFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.concurrent.thread

private val TAG = "MAIN_ACTIVITY"
lateinit var db: AppDatabase
lateinit var prefs: SharedPreferences

class MainActivity : AppCompatActivity() {
    private lateinit var mDrawerToggle: ActionBarDrawerToggle
    private lateinit var mFragmentManager: FragmentManager
    private lateinit var mDrawer: NavigationView
    private lateinit var mDrawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         *  Singletons
         */
        db = App.getDatabase(applicationContext)
        prefs = App.getPrefs(applicationContext)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.background = null
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerToggle = ActionBarDrawerToggle(this, mDrawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close).apply {
            isDrawerSlideAnimationEnabled = false
            syncState()
        }
        mDrawerLayout.addDrawerListener(mDrawerToggle)
        mDrawer = findViewById(R.id.navDrawer)
        mDrawer.bringToFront()

        firstTimeLoginPrompt()
        setGreeting()

        mDrawer.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item_tabs -> {
                    mFragmentManager
                        .beginTransaction()
                        .replace(R.id.main_container, DashboardFragment())
                        .commit()
                    it.isChecked = true
                    mDrawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_item_settings -> {
                    mFragmentManager
                        .beginTransaction()
                        .replace(R.id.main_container, SettingsFragment())
                        .commit()
                    it.isChecked = true
                    mDrawerLayout.closeDrawer(GravityCompat.START)
                    supportActionBar?.setBackgroundDrawable(null)
                    true
                }
                else -> false
            }
        }

        mFragmentManager = supportFragmentManager.also {
            it.beginTransaction()
                .add(R.id.main_container, DashboardFragment())
                .commit()
            mDrawer.setCheckedItem(R.id.nav_item_tabs)
        }
    }

    /**
     * Options menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.overflow_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Drawer menu
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        // Overflow menu
//        return when (item.itemId) {
//            R.id.menu_help -> {
//                startActivity(Intent(this, HelpActivity::class.java))
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
        return true
    }

    override fun onBackPressed() {
        when {
            // Close left navigation menu
            mDrawerLayout.isDrawerOpen(GravityCompat.START) -> mDrawerLayout.closeDrawer(GravityCompat.START)
            // Close right navigation menu
            mDrawerLayout.isDrawerOpen(GravityCompat.END) -> mDrawerLayout.closeDrawer(GravityCompat.END)
            else -> super.onBackPressed()
        }
    }

    /**
     * Private functions
     */
    private fun firstTimeLoginPrompt() {
        val username = prefs.getString("USERNAME", null)
        if (username == null) {
            val usernameInput = basicEditText(this)
//                .apply { background = null }

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
                        val firstMember = Member(null, usernameInput.text.toString())
                        prefs.edit().putString("USERNAME", firstMember.name).apply()
                        setGreeting()

                        thread {
                            val newTab = Tab(null, "Sample Tab")
                            db.tabDao().insert(newTab)
                            db.memberDao().insert(firstMember)
                        }

                        this.dismiss()
                    }
                }
            }
        }
    }

    private fun setGreeting() {
        val hv = mDrawer.getHeaderView(0).findViewById<TextView>(R.id.navHeaderTextPrimary)
        val name = prefs.getString("USERNAME", "Guest")
        val c = Calendar.getInstance()

        when (c.get(Calendar.HOUR_OF_DAY)) {
            in 0 until 12 -> hv.text = "Good morning, $name"
            in 12 until 18 -> hv.text = "Good afternoon, $name"
            else -> hv.text = "Good evening, $name"
        }
    }
}