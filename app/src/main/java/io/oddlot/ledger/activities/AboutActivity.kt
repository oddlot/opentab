package io.oddlot.ledger.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import io.oddlot.ledger.R
import io.oddlot.ledger.utils.BasicDialog

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).apply {
            title = getString(R.string.actionbar_overflow_about)
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tvLicenses = findViewById<TextView>(R.id.licenses)
        val tvPrivacy = findViewById<TextView>(R.id.privacy)
        val tvFeedback = findViewById<TextView>(R.id.feedback)
        val tvRateMe = findViewById<TextView>(R.id.rateMe)

        tvLicenses.setOnClickListener {
            val builder = AlertDialog.Builder(this)
                .setTitle("Ticker")
                .setMessage(
                    "Copyright 2016 Robinhood Markets, Inc.\n\n" +
                    """Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
            
http://www.apache.org/licenses/LICENSE-2.0
            
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.""")
                .create()

            builder.show()

            // Set alert message font
            builder.window?.findViewById<TextView>(android.R.id.message)
                ?.typeface = ResourcesCompat.getFont(this, R.font.quicksand)
        }

        tvPrivacy.setOnClickListener {
            val dialog = BasicDialog(this, "Privacy Policy").apply {
                val appName = context.getString(R.string.AppName)
                setMessage("$appName app only stores data on your personal device. Your information is never sold to nor shared with any third parties.")
            }

            dialog.show()

            // Set alert message font
            dialog.window?.findViewById<TextView>(android.R.id.message)
                ?.typeface = ResourcesCompat.getFont(this, R.font.quicksand)
        }

        tvFeedback.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_TITLE, "Send Email")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.my_email_address)))

                val appname = getString(R.string.AppName)
                putExtra(Intent.EXTRA_SUBJECT, "[Feedback] $appname")
            }

            startActivity(Intent.createChooser(intent, "Send Email"))
        }
        tvRateMe.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getString(R.string.app_store_link))
            }
            startActivity(intent)
        }
    }
}