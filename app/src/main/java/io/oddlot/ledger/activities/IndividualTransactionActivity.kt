package io.oddlot.ledger.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import io.oddlot.ledger.R
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.data.Transaction
import io.oddlot.ledger.parcelables.TabParcelable
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


class TransactionActivity : AppCompatActivity() {
    val TAG = "ADD_ITEM_ACTIVITY"
    lateinit var newExpense: Transaction
    lateinit var payerName: String
    lateinit var expenseDescription: String
    lateinit var itemDate: Date
    private lateinit var tabParcelable: TabParcelable
    private lateinit var mUsername: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_individual_transaction)
//        setContentView(R.layout.activity_create_expense)

        /*
         Member variables
         */
        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
        mUsername = prefs.getString("USERNAME", "null")!!

        val paidBySpinner: Spinner = findViewById(R.id.txnTypeSpinner)

        CoroutineScope(Main).launch {
            /*
            Toolbar
             */
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = "New Transaction"
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

            /*
            Type Switch
             */
//            val txnTypeSwitch = findViewById<SwitchCompat>(R.id.txnTypeSwitch)
//            txnTypeSwitch.text = "Paid"
//            txnTypeSwitch.textOff = "Owed"
//
//            txnTypeSwitch.setOnCheckedChangeListener { button, isChecked ->
//
//            }

            /*
            Amount Field
             */
            val amount = findViewById<TextView>(R.id.amountPaid)
            amount.isFocusable = true
            amount.requestFocus()

            val adapter = ArrayAdapter<String>(this@TransactionActivity, android.R.layout.simple_spinner_item, arrayOf(mUsername, tabParcelable.name))
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            paidBySpinner.adapter = adapter
            paidBySpinner.setSelection(0)
        }

//        ArrayAdapter.createFromResource(
//            this,
//            R.array.type_array,
//            android.R.layout.simple_spinner_item)
//            .also { adapter ->
//            // Specify the layout to use when the list of choices appears
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//            // Apply the adapter to the spinner
//            paidBySpinner.adapter = adapter
//        }
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
                    payerName = txnTypeSpinner.selectedItem.toString()

                    CoroutineScope(IO).launch {
                        try {
                            if (amountPaid.text.isBlank()) throw IllegalArgumentException("Amount required") // throw error if user didn't enter an amount

                            var itemAmount = amountPaid.text.toString().toDouble()

                            itemAmount = if (payerName == mUsername) itemAmount else itemAmount * -1.0 // Convert to negative if not paid by owner
//                    itemAmount = if (mPaidBy == "Debit") itemAmount else itemAmount * -1.0 // Convert to negative if "Credit"
                            expenseDescription = editDescription.text.toString()

                            newExpense = Transaction(null, tabParcelable.id, itemAmount, expenseDescription,
                                Utils.millisFromDateString(
                                    datePicker.text.toString(), "yyyy/MM/dd"
                                )
                            )
                            db.itemDao().insert(newExpense)
                        }
                        catch (e: IllegalArgumentException) {
                            CoroutineScope(Main).launch {
                                Toast.makeText(applicationContext, "Amount required", Toast.LENGTH_LONG).show()
                            }
                        }

                        withContext(Main) {
                            // Redirect to Tab Activity
                            Intent(this@TransactionActivity, IndividualTabActivity::class.java).apply {
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