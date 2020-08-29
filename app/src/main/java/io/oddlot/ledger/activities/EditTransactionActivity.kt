package io.oddlot.ledger.activities

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.MenuItem
import android.widget.*
import androidx.core.app.NavUtils
import io.oddlot.ledger.R
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.parcelables.ItemParcelable
import io.oddlot.ledger.data.Transaction
import io.oddlot.ledger.parcelables.TabParcelable
import kotlinx.android.synthetic.main.activity_edit_expense.*
import kotlin.concurrent.thread

class EditItemActivity : AppCompatActivity() {
    private val TAG = "EDIT_ITEM_ACTIVITY"
    private lateinit var itemDateString: String // "2019-10-14"
    private lateinit var itemParcelable: ItemParcelable
    private lateinit var tabParcelable: TabParcelable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_expense)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = "Edit Item"
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        // 2. parcelables
        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
        itemParcelable = intent.getParcelableExtra("ITEM_PARCELABLE") as ItemParcelable
        itemDateString = Utils.dateStringFromMillis(itemParcelable.date, "yyyy/MM/dd")

        val itemType = if (itemParcelable.amount > 0.0) "Debit" else "Credit"
        val mItemAmount = if (itemParcelable.amount > 0.0) itemParcelable.amount else itemParcelable.amount * -1.0

        val itemDescription = itemParcelable.description
        val tabId = itemParcelable.tabId
        val itemId = itemParcelable.itemId
        var itemDate = itemParcelable.date // "2019-10-14"

        val dateView = findViewById<TextView>(R.id.editItemDate).apply {
            text = Utils.dateStringFromMillis(itemDate).replace("-", "/")
        }

        amountPaid.text = SpannableStringBuilder(if (mItemAmount > 0.0) mItemAmount.toString() else (mItemAmount * -1.0).toString())
        editDescription.text = SpannableStringBuilder(itemDescription)

        editItemDate.setOnClickListener {
            DatePickerDialog(this).apply {
                this.updateDate(itemDateString.slice(0..3).toInt(), itemDateString.slice(5..6).toInt() - 1, itemDateString.slice(8..9).toInt())
                this.setOnDateSetListener { view, year, month, day ->
                    // Get month and day string representations
                    var month = (month + 1).toString()
                    var day = day.toString()

                    // Zero pad month and day
                    if (month.length < 2) month = "0" + month
                    if (day.length < 2) day = "0" + day

                    val dateStr = "$year/$month/$day".also {
                        dateView.text = it
                        itemDateString = it
                    }

                    itemParcelable.date = Utils.millisFromDateString(dateStr.replace("/", "-"))
                    itemDate = Utils.millisFromDateString(dateStr.replace("/", "-"))
                }
                this.show()
            }
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.type_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            paidBySpinner.adapter = adapter

            // Set Saved Type Selection
            paidBySpinner.setSelection(adapter.getPosition(itemType))
        }

        itemEditBtn.setOnClickListener {
            // Local Item Update
            var itemAmount = amountPaid.text.toString().toDouble() // Positive amount if "Debit"
            if (paidBySpinner.selectedItem == "Credit") itemAmount *= -1.0 // Negative if "Credit"

            val updateItem = Transaction(itemId, tabId, itemAmount, editDescription.text.toString(), itemDate)

            thread {
                db.itemDao().insert(updateItem)
//                db.tabDao().updateTabBalance(tabParcelable.id, tabBalance += updateItem.amount)
            }

            // Redirect to Tab Activity
            Intent(this, IndividualTabActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // finish all activities on top of individual tab activity
//                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // keeps individual tab activity instead of recreating

                putExtra("TAB_PARCELABLE", tabParcelable)
                startActivity(this)
            }

            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = intent.putExtra("TAB_PARCELABLE", tabParcelable)
                NavUtils.navigateUpTo(this, intent)

                true
            } else -> {
                true
            }
        }
    }
}