//package io.oddlot.ledger.activities
//
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.content.SharedPreferences
//import android.view.Menu
//import android.view.MenuItem
//import android.widget.FrameLayout
//import android.widget.TextView
//import androidx.appcompat.app.ActionBarDrawerToggle
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatDelegate
//import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
//import androidx.core.view.GravityCompat
//import androidx.drawerlayout.widget.DrawerLayout
//import androidx.fragment.app.FragmentManager
//import com.google.android.material.navigation.NavigationView
//import io.oddlot.ledger.App
//import io.oddlot.ledger.R
//import io.oddlot.ledger.utils.basicEditText
//import io.oddlot.ledger.data.*
//import io.oddlot.ledger.fragments.MainFragment
//import io.oddlot.ledger.fragments.SettingsFragment
//import java.util.*
//import kotlin.concurrent.thread
//
//private val TAG = "MAIN_ACTIVITY"
//lateinit var db: AppDatabase
//lateinit var prefs: SharedPreferences
//
//class MainActivity : AppCompatActivity() {
//    private lateinit var drawerToggle: ActionBarDrawerToggle
//    private lateinit var fragmentManager: FragmentManager
//    private lateinit var drawerNav: NavigationView
//    private lateinit var drawerLayout: DrawerLayout
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
//
//        /**
//         *  Singletons
//         */
//        db = App.getDatabase(applicationContext)
//        prefs = App.getPrefs(applicationContext)
//
//        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
//        toolbar.background = null
//        setSupportActionBar(toolbar)
//        supportActionBar?.title = ""
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        /*
//        Drawer configuration
//         */
//        drawerLayout = findViewById(R.id.drawer_layout)
//        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close).apply {
//            isDrawerSlideAnimationEnabled = false
//            syncState()
//        }
//        drawerLayout.addDrawerListener(drawerToggle)
//
//        drawerNav = findViewById(R.id.nav_drawer_menu)
//        drawerNav.setCheckedItem(R.id.nav_item_tabs)
//        drawerNav.bringToFront()
//        drawerNav.setNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.nav_item_tabs -> {
//                    fragmentManager
//                        .beginTransaction()
//                        .replace(R.id.main_container, MainFragment())
//                        .commit()
//                    it.isChecked = true
//                    drawerLayout.closeDrawer(GravityCompat.START)
//                    true
//                }
//                R.id.nav_item_settings -> {
//                    fragmentManager
//                        .beginTransaction()
//                        .replace(R.id.main_container, SettingsFragment())
//                        .commit()
//                    it.isChecked = true
//                    drawerLayout.closeDrawer(GravityCompat.START)
//                    supportActionBar?.setBackgroundDrawable(null)
//                    true
//                }
//                else -> false
//            }
//        }
//
//        /*
//        Configure fragment for displaying tabs
//         */
//        fragmentManager = supportFragmentManager.also {
//            it.beginTransaction()
//                .add(R.id.main_container, MainFragment())
//                .commit()
//
//        }
//
//        checkIfFirstLaunch()
//        setGreeting()
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.main_overflow_menu, menu)
//
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Drawer menu
//        if (drawerToggle.onOptionsItemSelected(item)) {
//            return true
//        }
//
//        // Overflow menu
////        return when (item.itemId) {
////            R.id.menu_help -> {
////                startActivity(Intent(this, HelpActivity::class.java))
////                true
////            }
////            else -> super.onOptionsItemSelected(item)
////        }
//        return true
//    }
//
//    override fun onBackPressed() {
//        when {
//            // Close left navigation menu
//            drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawer(GravityCompat.START)
//            // Close right navigation menu
//            drawerLayout.isDrawerOpen(GravityCompat.END) -> drawerLayout.closeDrawer(GravityCompat.END)
//            else -> super.onBackPressed()
//        }
//    }
//
//    /**
//     * Private functions
//     */
//    private fun checkIfFirstLaunch() {
//        val username = prefs.getString("USERNAME", null)
//        if (username == null) {
//            val usernameInput = basicEditText(this)
////                .apply { background = null }
//
//            val container = FrameLayout(this).apply {
//                addView(usernameInput)
//            }
//
//            val builder = AlertDialog.Builder(this).apply {
//                setView(container)
//                setTitle("Hi! What's your name?")
//                setCancelable(false)
//                setPositiveButton("OK") { dialog, which ->
//                    // Caught in event listener below
//                }
//
//                // Unreachable if setCancelable = false
//                setOnCancelListener {
//                    moveTaskToBack(true)
//                    android.os.Process.killProcess(android.os.Process.myPid())
//                    System.exit(1)
//                }
//            }
//
//            /* Catch Positive button event */
//            builder.create().apply {
//                show()
//                getButton(-1).setOnClickListener {
//                    if (usernameInput.text.isEmpty()) {
//                        usernameInput.error = "I'm sorry, didn't catch your name!"
//                    }
//                    else {
//                        val firstMember = Member(null, usernameInput.text.toString())
//                        prefs.edit().putString("USERNAME", firstMember.name).apply()
//                        setGreeting()
//
//                        thread {
//                            val newTab = Tab(null, "Sample Tab")
//                            db.tabDao().insert(newTab)
//                            db.memberDao().insert(firstMember)
//                        }
//
//                        this.dismiss()
//                    }
//                }
//            }
//        }
//    }
//
//    private fun setGreeting() {
//        val hv = drawerNav.getHeaderView(0).findViewById<TextView>(R.id.navHeaderTextPrimary)
//        val name = prefs.getString("USERNAME", "Guest")
//        val c = Calendar.getInstance()
//
//        hv.text = "$name"
//
////        when (c.get(Calendar.HOUR_OF_DAY)) {
////            in 0 until 12 -> hv.text = "Good morning, $name"
////            in 12 until 18 -> hv.text = "Good afternoon, $name"
////            else -> hv.text = "Good evening, $name"
////        }
//    }
//}