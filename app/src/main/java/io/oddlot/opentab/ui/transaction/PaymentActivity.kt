package io.oddlot.opentab.ui.transaction

import android.app.ActivityOptions
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
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
import io.oddlot.opentab.utils.toDateString
import kotlinx.android.synthetic.main.activity_transaction.*
import kotlinx.android.synthetic.main.activity_transaction.amountPaid
import kotlinx.android.synthetic.main.activity_transaction.datePicker
import kotlinx.android.synthetic.main.activity_transaction.transactionDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName

    private var txnParcelable: TransactionParcelable? = null
    private var tabParcelable: TabParcelable? = null
    private var paymentDate: Date = Date()
    private var calendar: GregorianCalendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "New Payment"
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE")
        intent.getParcelableExtra<TransactionParcelable>("TXN_PARCELABLE")?.let {
            supportActionBar?.title = "Edit Payment"
            paymentDate = Date(it.date)
            datePicker.text = StringUtils.dateStringFromMillis(paymentDate.time, "yyyy/MM/dd")

            txnParcelable = it
        }

        loadUiData()
    }

    private fun loadUiData() {
        debitButton.text = "I Paid"
        creditButton.text = "I Received"

        txnParcelable?.let { payment ->
            if (payment.isTransfer == 1) {
                if (payment.amount < 0f) {
                    credDebToggle.check(R.id.creditButton)
                    amountPaid.setText((payment.amount * -1).toString())
                } else {
                    credDebToggle.check(R.id.debitButton)
                    amountPaid.setText((payment.amount).toString())
                }
            }
        }

        val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dateString = paymentDate.time.toDateString("yyyy/MM/dd")

        datePicker.text = StringUtils.dateStringFromMillis(paymentDate.time, "yyyy/MM/dd")
        datePicker.setOnClickListener {
            if (calendar == null) {
                calendar = GregorianCalendar()

                txnParcelable?.let {
                    calendar?.time = Date(it.date)
                }
            }

            val year = calendar!!.get(Calendar.YEAR)
            val month = calendar!!.get(Calendar.MONTH)
            val day = calendar!!.get(Calendar.DAY_OF_MONTH)

            var dpd = DatePickerDialog(this, null, year, month, day)
            dpd.setOnDateSetListener { view, year, month, day ->
                // Set month and day string variables
                var month = (month + 1).toString()
                var day = day.toString()

                // Zero Pad
                if (month.length < 2) month = "0" + month
                if (day.length < 2) day = "0" + day

                var dialogDate = "$year/$month/$day"
                datePicker.text = dialogDate
//                paymentDate = formatter.parse(dialogDate)
                paymentDate = formatter.parse(dialogDate)
            }

            dpd.show()
        }

        CoroutineScope(IO).launch {
            val tabs = db.tabDao().getAll()
            val tabIndex = tabs.map { tab -> tab.id }.indexOf(tabParcelable?.id)
            tabSpinner.adapter = ArrayAdapter<String>(
                this@PaymentActivity,
                android.R.layout.simple_spinner_dropdown_item,
                tabs.map { tab -> tab.name }
            )

            tabSpinner.setSelection(tabIndex)
        }

        transactionDescription.setText(txnParcelable?.description)
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

                CoroutineScope(IO).launch {
                    val transactionAmount = if (creditButton.isChecked) {
                        amountPaid.text.toString().toDouble() * -1.0
                    } else {
                        amountPaid.text.toString().toDouble()
                    }

                    val payment = Transaction(
                        txnParcelable?.id,
                        tabParcelable!!.id,
                        transactionAmount,
                        transactionDescription.text.toString(),
                        StringUtils.millisFromDateString(datePicker.text.toString(), "yyyy/MM/dd"),
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