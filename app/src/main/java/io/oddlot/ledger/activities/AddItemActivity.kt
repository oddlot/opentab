package io.oddlot.ledger.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import io.oddlot.ledger.R
import io.oddlot.ledger.classes.Utils
import io.oddlot.ledger.data.Expense
import io.oddlot.ledger.parcelables.TabParcelable
import kotlinx.android.synthetic.main.activity_add_item.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


class AddItemActivity : AppCompatActivity() {
    val TAG = "ADD_ITEM_ACTIVITY"
    lateinit var newExpense: Expense
    lateinit var mPaidBy: String
    lateinit var itemDescription: String
    lateinit var itemDate: Date
    private lateinit var tabParcelable: TabParcelable
    private lateinit var mUsername: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        /**
         * Member variables
         */
        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
        mUsername = prefs.getString("USERNAME", "null")!!

        /**
         * Toolbar
         */
        CoroutineScope(Main).launch {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Add Item"
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            /**
             * Date Picker Dialog
             */
//            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd") // min API level 26
            val formatter = SimpleDateFormat("yyyy/MM/dd")

//            itemDate = LocalDate.now() // disabled Dec 3
//            val dateString = itemDate.format(formatter) // removed Dec 3

            itemDate = Date()
            val dateString = formatter.format(itemDate)

            addItemDatePicker.text = dateString

            addItemDatePicker.setOnClickListener {
                var dpd = DatePickerDialog(this@AddItemActivity)
                dpd.setOnDateSetListener { view, year, month, day ->
                    // Set month and day string variables
                    var month = (month + 1).toString()
                    var day = day.toString()

                    // Zero Pad
                    if (month.length < 2) month = "0" + month
                    if (day.length < 2) day = "0" + day

                    var dialogDate = "$year/$month/$day"
                    addItemDatePicker.text = dialogDate
//                    itemDate = LocalDate.parse(dialogDate, formatter)
                    itemDate = formatter.parse(dialogDate)
                }

                dpd.show()
            }

            val amount = findViewById<TextView>(R.id.totalAmount)
            amount.isFocusable = true
            amount.requestFocus()

            /**
             * Payer Spinner
             */
            val paidBySpinner: Spinner = findViewById(R.id.paidBySpinner)
            val adapter = ArrayAdapter<String>(this@AddItemActivity, android.R.layout.simple_spinner_item, arrayOf(mUsername, tabParcelable.name))
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

        // Submit Button Click Listener
        itemSubmitBtn.setOnClickListener {
            mPaidBy = paidBySpinner.selectedItem.toString()

            CoroutineScope(IO).launch {
                try {
                    if (totalAmount.text.isBlank()) throw IllegalArgumentException("Amount required")

                    var itemAmount = totalAmount.text.toString().toDouble()

                    itemAmount = if (mPaidBy == mUsername) itemAmount else itemAmount * -1.0 // Convert to negative if not paid by owner
//                    itemAmount = if (mPaidBy == "Debit") itemAmount else itemAmount * -1.0 // Convert to negative if "Credit"
                    itemDescription = itemDescriptionInput.text.toString()

                    newExpense = Expense(null, tabParcelable.id, itemAmount, itemDescription,
                        Utils.millisFromDateString(
                            addItemDatePicker.text.toString(), "yyyy/MM/dd"
                        )
                    )
                    db.itemDao().insert(newExpense)
                }
                catch (e: IllegalArgumentException) {
                    Toast.makeText(it.context, "Amount is required", Toast.LENGTH_LONG).show()
                }

                withContext(Main) {
                    // Redirect to Tab Activity
                    Intent(this@AddItemActivity, IndividualTabActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("TAB_PARCELABLE", tabParcelable)
                        startActivity(this)
                    }

                    finish()
                }
            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = intent.putExtra("TAB_PARCELABLE", intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable)
                NavUtils.navigateUpTo(this, intent)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}