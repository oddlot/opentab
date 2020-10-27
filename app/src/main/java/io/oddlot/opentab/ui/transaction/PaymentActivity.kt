package io.oddlot.opentab.ui.transaction

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import io.oddlot.opentab.R
import io.oddlot.opentab.data.Transaction
import io.oddlot.opentab.db
import io.oddlot.opentab.parcelables.TabParcelable
import io.oddlot.opentab.parcelables.TransactionParcelable
import io.oddlot.opentab.ui.tab.TabActivity
import io.oddlot.opentab.utils.StringUtils
import kotlinx.android.synthetic.main.activity_transaction.*
import kotlinx.android.synthetic.main.activity_transaction.amountPaid
import kotlinx.android.synthetic.main.activity_transaction.datePicker
import kotlinx.android.synthetic.main.activity_transaction.transactionDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class PaymentActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName

    var paymentParcelable: TransactionParcelable? = null
    var tabParcelable: TabParcelable? = null
    var paymentDate: Date = Date()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE")
        intent.getParcelableExtra<TransactionParcelable>("TXN_PARCELABLE")?.let {
            creditSwitch.isChecked = (it.amount > 0.0)
            paymentDate = Date(it.date)
            datePicker.text = StringUtils.dateStringFromMillis(paymentDate.time)
            paymentParcelable = it
        }

        loadUiData()
    }

    private fun loadUiData() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Payment"
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        creditText.text = "Paid"
        debitText.text = "Received"
        datePicker.text = StringUtils.dateStringFromMillis(paymentDate.time)
        tabSpinner.setSelection(0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater

        menu!!.add("").apply { // Add tick submit icon
            icon = getDrawable(R.drawable.ic_check_white_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        inflater.inflate(R.menu.ticket_overflow, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = intent.putExtra("TAB_PARCELABLE", tabParcelable)
                NavUtils.navigateUpTo(this, intent)

                true
            }
            0 -> { // Submit

                if (amountPaid.text.isBlank() || amountPaid.text.toString().toFloat() == 0f) {
                    Toast.makeText(applicationContext, "Non-zero amount required", Toast.LENGTH_SHORT).show()

                    return false
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val transactionAmount = if (creditSwitch.isChecked == false) {
                        amountPaid.text.toString().toDouble() * -1.0
                    } else {
                        amountPaid.text.toString().toDouble()
                    }

                    val payment = Transaction(
                        paymentParcelable?.id,
                        tabParcelable!!.id,
                        transactionAmount,
                        transactionDescription.text.toString(),
                        StringUtils.millisFromDateString(datePicker.text.toString()),
                        true
                    )
                    db.transactionDao().insert(payment)

                    // Redirect to Tab Activity
                    Intent(this@PaymentActivity, TabActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("NEW_TASK_ON_BACK", true)
                        putExtra("TAB_PARCELABLE", tabParcelable)
                        startActivity(this)
                    }
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, TabActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("TAB_PARCELABLE", tabParcelable)
        }

        startActivity(intent, ActivityOptions.makeCustomAnimation(this, 0, R.anim.exit_right).toBundle())

        finish()
    }
}