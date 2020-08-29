package io.oddlot.ledger.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import kotlinx.android.synthetic.main.activity_create_expense.editDescription
import kotlinx.android.synthetic.main.activity_individual_transaction.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*


class IndividualTransactionActivity : AppCompatActivity() {
    val TAG = "ADD_TRANSACTION_ACTIVITY"

    lateinit var newTxn: Transaction
    lateinit var payerName: String
    private var txnAmount: Double? = null
    private var txnDescription: String? = null
    lateinit var itemDate: Date
    lateinit var tabs: List<Tab>
    private lateinit var selectedTab: Tab
    private lateinit var selectedPayee: Member
    private var txnTabId = 0
    private lateinit var tabParcelable: TabParcelable
    private var txnParcelable: TransactionParcelable? = null
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
        Background work
         */
        CoroutineScope(IO).launch {
            selectedTab = db.tabDao().get(tabParcelable.id)
            tabs = db.tabDao().getAll()

            CoroutineScope(Main).launch {
                /*
                Tab Spinner
                 */
                var tabIndex = tabs.map { tab -> tab.id }.indexOf(tabParcelable.id)
                tabSpinner.adapter = ArrayAdapter<String>(
                    this@IndividualTransactionActivity, android.R.layout.simple_spinner_dropdown_item, tabs.map { tab -> tab.name }
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
                            selectedTab = tabs.get(position)
                            selectedPayee = db.memberDao().getMemberByName(username)!!

                            CoroutineScope(Main).launch {
                                paidBySpinner.adapter = ArrayAdapter<String>(
                                    this@IndividualTransactionActivity,  android.R.layout.simple_spinner_dropdown_item, listOf(username, selectedTab.name)
                                )
                            }

                            Log.d(TAG, "Spinner item selected")
                            Log.d(TAG, "Tab Name: ${selectedTab.name}")
                            Log.d(TAG, "Tab ID: ${selectedTab.id}")
                        }
                    }
                }

                /*
                Amount Field
                 */
                val etAmount = findViewById<TextView>(R.id.amountPaid)
                etAmount.isFocusable = true
                etAmount.text = txnAmount.toString()
                etAmount.requestFocus()

                val adapter = ArrayAdapter<String>(this@IndividualTransactionActivity, android.R.layout.simple_spinner_dropdown_item, listOf(username, tabParcelable.name))
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                paidBySpinner.adapter = adapter
                paidBySpinner.setSelection(1)
//                paidBySpinner.setSelection(if (txnAmount == null) 0 else if (txnAmount!! > 0.0) 0 else 1)
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

//            itemDate = LocalDate.now() // disabled Dec 3
//            val dateString = itemDate.format(formatter) // removed Dec 3

        itemDate = Date()
        val dateString = formatter.format(itemDate)

        datePicker.text = dateString

        datePicker.setOnClickListener {
            var dpd = DatePickerDialog(this@IndividualTransactionActivity)
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
//                val intent = intent.putExtra("TAB_PARCELABLE", intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable)
                val intent = intent.putExtra("TAB_PARCELABLE", tabParcelable)
                NavUtils.navigateUpTo(this, intent)

                true
            }
            0 -> {
                try {
                    payerName = paidBySpinner.selectedItem.toString()

                    CoroutineScope(IO).launch {
                        try {
                            if (amountPaid.text.isBlank()) throw IllegalArgumentException("Amount required") // throw error if user didn't enter an amount

                            var txnAmount = amountPaid.text.toString().toDouble()

                            // Convert to negative if not paid by owner
                            txnAmount = if (payerName != username) txnAmount * -1.0 else txnAmount

                            txnDescription = editDescription.text.toString()

                            newTxn = Transaction(null, tabParcelable.id, txnAmount, txnDescription,
                                Utils.millisFromDateString(
                                    datePicker.text.toString(), "yyyy/MM/dd"
                                )
                            )
                            db.itemDao().insert(newTxn)
                        }
                        catch (e: IllegalArgumentException) {
                            CoroutineScope(Main).launch {
                                Toast.makeText(applicationContext, "Amount required", Toast.LENGTH_LONG).show()
                            }
                        }

                        withContext(Main) {
                            // Redirect to Tab Activity
                            Intent(this@IndividualTransactionActivity, IndividualTabActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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