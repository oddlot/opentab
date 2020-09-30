package io.oddlot.ledger.activities

import android.app.ActivityOptions
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import io.oddlot.ledger.PreferenceKeys
import io.oddlot.ledger.R
import io.oddlot.ledger.data.Member
import io.oddlot.ledger.data.Tab
import io.oddlot.ledger.utils.StringUtils
import io.oddlot.ledger.data.Transaction
import io.oddlot.ledger.db
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.parcelables.TransactionParcelable
import io.oddlot.ledger.viewmodels.TransactionViewModel
import kotlinx.android.synthetic.main.activity_create_expense.amountPaid
import kotlinx.android.synthetic.main.activity_create_expense.datePicker
import kotlinx.android.synthetic.main.activity_transaction.*
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

class TransactionActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName
    private lateinit var viewModel: TransactionViewModel

    private var txnParcelable: TransactionParcelable? = null
    private var txnAmount: Double? = null
    private var txnDescription: String? = null
    private lateinit var payerName: String
    private var itemDate = Date()
    private lateinit var tabs: List<Tab>
    private lateinit var selectedTab: Tab
    private lateinit var selectedPayee: Member
    private lateinit var tabParcelable: TabParcelable
    private lateinit var userName: String
    private var isTransfer: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        userName = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(PreferenceKeys.USER_NAME, "null")!!
        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
        intent.getParcelableExtra<TransactionParcelable>("TXN_PARCELABLE")?.let {
            txnParcelable = it
            txnAmount = it.amount
            txnDescription = it.description
            itemDate = Date(it.date)
            payerName = if (txnAmount!! > 0.0) userName else tabParcelable.name
        }

        viewModel = ViewModelProvider(this).get(TransactionViewModel::class.java).also { vm ->
            txnParcelable?.let { vm.init(it) } // initialize with parcelable data if one exists
        }

        Log.d(TAG, viewModel.toString())

        /*
        Tab, Amount, and Payer views
         */
        CoroutineScope(IO).launch {
            selectedTab = db.tabDao().tabById(tabParcelable.id)
            tabs = db.tabDao().allTabs()

            CoroutineScope(Main).launch {
                /*
                Tab Spinner
                 */
                val tabIndex = tabs.map { tab -> tab.id }.indexOf(tabParcelable.id)
                tabSpinner.adapter = ArrayAdapter<String>(
                    this@TransactionActivity,
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
                            selectedPayee = db.memberDao().getMemberByName(userName)!!

                            withContext(Main) {
                                paidBySpinner.adapter = ArrayAdapter<String>(
                                    this@TransactionActivity,  android.R.layout.simple_spinner_dropdown_item, listOf(userName, selectedTab.name)
                                )
                                txnParcelable?.let {
                                    paidBySpinner.setSelection(listOf(userName, selectedTab.name).indexOf(payerName))
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
                    this@TransactionActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(userName, tabParcelable.name)
                )

                /*
                Is transfer switch
                 */
                txnParcelable?.let {
                    if (it.isTransfer == 1) isTransfer = true
                    Log.d(TAG, it.isTransfer.toString())
                    isTransferSwitch.isChecked = isTransfer
                }
                isTransferSwitch.setOnCheckedChangeListener { btn, checked ->
                    Log.d(TAG, "Switch checked! $checked")
                    isTransfer = checked
                }

                txnDescription?.let { transactionDescription.text = SpannableStringBuilder(it) }
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

        val dateString = formatter.format(itemDate)

        datePicker.text = dateString
        datePicker.setOnClickListener {
            var dpd = DatePickerDialog(this@TransactionActivity)
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
                            if (payerName != userName && txnAmount > 0.0 ) {
                                txnAmount *= -1.0
                            }

                            txnDescription = transactionDescription.text.toString()

                            val txn = Transaction(txnParcelable?.id, selectedTab.id ?: tabParcelable.id, txnAmount, txnDescription,
                                StringUtils.millisFromDateString(
                                    datePicker.text.toString(), "yyyy/MM/dd"
                                ),
                                isTransfer
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
                            Intent(this@TransactionActivity, TabActivity::class.java).apply {
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

    override fun onBackPressed() {
        val intent = Intent(this, TabActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("TAB_PARCELABLE", tabParcelable)
        }

        startActivity(intent, ActivityOptions.makeCustomAnimation(this, 0, R.anim.exit_right).toBundle())

        finish()
    }
}