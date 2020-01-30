package io.oddlot.ledger.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.oddlot.ledger.R

private val TAG = "HELP_ACTIVITY"

class HelpActivity : AppCompatActivity() {
    private lateinit var mSharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // Action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Help"
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mSharedPrefs = getSharedPreferences("io.oddlot.ledger.prefs", MODE_PRIVATE)

    }
}