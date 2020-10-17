package io.oddlot.opentab.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.oddlot.opentab.R

private val TAG = "HELP_ACTIVITY"

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // Action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Help"
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


    }
}