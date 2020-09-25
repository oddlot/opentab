package io.oddlot.ledger.activities

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.oddlot.ledger.R
import io.oddlot.ledger.fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.navigationIcon?.setTint(getColor(R.color.DefaultWhite))

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

//        val drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close).apply {
//            isDrawerSlideAnimationEnabled = false
//            syncState()
//        }

        // Settings Fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settingsContainer, SettingsFragment())
            .commit()
    }
}