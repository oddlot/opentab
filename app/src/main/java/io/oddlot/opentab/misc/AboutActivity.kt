package io.oddlot.opentab.misc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import io.oddlot.opentab.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).apply {
            title = getString(R.string.actionbar_overflow_about)
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val licensesTV = findViewById<TextView>(R.id.licenses)
        val privacyTV = findViewById<TextView>(R.id.privacy)
        val feedbackTV = findViewById<TextView>(R.id.feedback)
        val rateMeTV = findViewById<TextView>(R.id.rateMe)

        licensesTV.setOnClickListener {
            val builder = AlertDialog.Builder(this)
                .setTitle("Notices for Libraries")
                .setMessage("${getString(R.string.AppName)} utilizes the following open source libraries:")
                .setView(R.layout.licenses)
                .create()

            builder.show()

            // Link to Robinhood license
            builder.findViewById<TextView>(R.id.robinhoodLicense)!!.apply {
                text = HtmlCompat.fromHtml(getString(R.string.link_ticker_license), HtmlCompat.FROM_HTML_MODE_LEGACY)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/robinhood/ticker#license"))
                    startActivity(intent)
                }
            }

            // Link to Material license
            builder.findViewById<TextView>(R.id.materialLicense)!!.apply {
                text = HtmlCompat.fromHtml(getString(R.string.link_material_license), HtmlCompat.FROM_HTML_MODE_LEGACY)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/material-components/material-components-android/blob/master/LICENSE"))
                    startActivity(intent)
                }
            }

            // Set alert message font
            builder.window?.findViewById<TextView>(android.R.id.message)
                ?.typeface = ResourcesCompat.getFont(this, R.font.quicksand)
        }

        privacyTV.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.AlertDialog).apply {
                val appName = context.getString(R.string.AppName)
                setTitle("Privacy Policy")
                setMessage("$appName is free to use and does not contain any ads or collect the personal data of its users.  "
//                    + "App data is not accessed by the developers of $appName or sold to third parties."
                    + "\n\nThis policy is subject to change and may be revised in future app updates or releases.")
            }
            val dialog = builder.show()

            // Set alert message font
            dialog.window?.findViewById<TextView>(android.R.id.message)
                ?.typeface = ResourcesCompat.getFont(this, R.font.quicksand)
        }

        feedbackTV.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_TITLE, "Send Email")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.my_email_address)))

                val appname = getString(R.string.AppName)
                putExtra(Intent.EXTRA_SUBJECT, "[Support/Feedback] $appname")
            }

            startActivity(Intent.createChooser(intent, "Send Email"))
        }
        rateMeTV.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getString(R.string.app_store_link))
            }
            startActivity(intent)
        }
    }
}