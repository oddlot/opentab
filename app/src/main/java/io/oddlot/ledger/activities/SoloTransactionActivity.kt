package io.oddlot.ledger.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import io.oddlot.ledger.R
import io.oddlot.ledger.data.Member
import io.oddlot.ledger.data.Tab
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.data.Transaction
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.parcelables.TransactionParcelable
import kotlinx.android.synthetic.main.activity_create_expense.amountPaid
import kotlinx.android.synthetic.main.activity_create_expense.datePicker
import kotlinx.android.synthetic.main.activity_individual_transaction.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity class for staging new and existing solo (individual) transactions.
 *
 * Notes:
 * - Paid By item must be set AFTER setting Tab item or it will be overridden
 */

class SoloTransactionActivity : AppCompatActivity() {
    private val TAG = this::class.java.name

    private var txnParcelable: TransactionParcelable? = null
    private var txnAmount: Double? = null
    private var txnDescription: String? = null
    private lateinit var payerName: String
    private lateinit var itemDate: Date
    private lateinit var tabs: List<Tab>
    private lateinit var selectedTab: Tab
    private lateinit var selectedPayee: Member
    private lateinit var tabParcelable: TabParcelable
    private lateinit var username: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_individual_transaction)

        /*
         Member variables
         */
        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
        username = prefs.getString("USERNAME", "null")!!

        intent.getParcelableExtra<TransactionParcelable>("TXN_PARCELABLE")?.let {
            txnParcelable = it
            txnAmount = it.amount
            txnDescription = it.description
            payerName = if (txnAmount!! > 0.0) username else tabParcelable.name
        }

        /*
        Tab, Amount, and Payer views
         */
        CoroutineScope(IO).launch {
            selectedTab = db.tabDao().get(tabParcelable.id)
            tabs = db.tabDao().getAll()

            CoroutineScope(Main).launch {
                /*
                Tab Spinner
                 */
                val tabIndex = tabs.map { tab -> tab.id }.indexOf(tabParcelable.id)
                tabSpinner.adapter = ArrayAdapter<String>(
                    this@SoloTransactionActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    tabs.map { tab -> tab.name }
                )

                tabSpinner.setSelection(tabIndex)
                tabSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        TODO("Not yet implemented")
                    }

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        CoroutineScope(IO).launch {
                            selectedTab = tabs[position]
                            selectedPayee = db.memberDao().getMemberByName(username)!!

                            withContext(Main) {
                                paidBySpinner.adapter = ArrayAdapter<String>(
                                    this@SoloTransactionActivity,  android.R.layout.simple_spinner_dropdown_item, listOf(username, selectedTab.name)
                                )
                                txnParcelable?.let {
                                    paidBySpinner.setSelection(listOf(username, selectedTab.name).indexOf(payerName))
                                }
                            }
                        }
                    }
                }

                /*
                Amount Field
                 */
                findViewById<TextView>(R.id.amountPaid).apply {
                    isFocusable = true
                    requestFocus()
                    txnAmount?.let {
                        this.text = if (it < 0.0) (it * -1.0).toString() else it.toString()
                    }
                }

                /*
                Paid By Spinner
                 */
                paidBySpinner.adapter = ArrayAdapter<String>(
                    this@SoloTransactionActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(username, tabParcelable.name)
                )

                txnDescription?.let { etDescription.text = SpannableStringBuilder(it) }
            }
        }

        /*
        Toolbar
         */
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = if (txnParcelable == null) "Add Transaction" else "Edit Transaction"
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        /*
        Date Picker Dialog
         */
//            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd") // min API level 26
        val formatter = SimpleDateFormat("yyyy/MM/dd")

        itemDate = Date()
        val dateString = formatter.format(itemDate)

        datePicker.text = dateString
        datePicker.setOnClickListener {
            var dpd = DatePickerDialog(this@SoloTransactionActivity)
            dpd.setOnDateSetListener { view, year, month, day ->
                // Set month and day string variables
                var month = (month + 1).toString()
                var day = day.toString()

                // Zero Pad
                if (month.length < 2) month = "0" + month
                if (day.length < 2) day = "0" + day

                var dialogDate = "$year/$month/$day"
                datePicker.text = dialogDate
//                    itemDate = LocalDate.parse(dialogDate, formatter)
                itemDate = formatter.parse(dialogDate)
            }

            dpd.show()
        }
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
                try {
                    payerName = paidBySpinner.selectedItem.toString()

                    CoroutineScope(IO).launch {
                        try {
                            if (amountPaid.text.isBlank()) throw IllegalArgumentException("Amount required") // throw error if user didn't enter an amount

                            var txnAmount = amountPaid.text.toString().toDouble()

                            /* Convert to negative if not paid by owner */
                            if (payerName != username && txnAmount > 0.0 ) {
                                txnAmount *= -1.0
                            }

                            txnDescription = etDescription.text.toString()

                            val txn = Transaction(txnParcelable?.id, selectedTab.id ?: tabParcelable.id, txnAmount, txnDescription,
                                Utils.millisFromDateString(
                                    datePicker.text.toString(), "yyyy/MM/dd"
                                )
                            )
                            db.transactionDao().insert(txn)
                        }
                        catch (e: IllegalArgumentException) {
                            CoroutineScope(Main).launch {
                                Toast.makeText(applicationContext, "Amount required", Toast.LENGTH_LONG).show()
                            }
                        }

                        withContext(Main) {
                            // Redirect to Tab Activity
                            Intent(this@SoloTransactionActivity, SoloTabActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("NEW_TASK_ON_BACK", true)
                                putExtra("TAB_PARCELABLE", tabParcelable)
                                startActivity(this)
                            }

                            finish()
                        }
                    }
                } catch (e: IllegalStateException) {

                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}