//package io.oddlot.ledger.activities
//
//import android.app.DatePickerDialog
//import android.content.Intent
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.text.SpannableStringBuilder
//import android.util.Log
//import android.view.Menu
//import android.view.MenuInflater
//import android.view.MenuItem
//import android.view.View
//import android.widget.*
//import androidx.core.app.NavUtils
//import io.oddlot.ledger.R
//import io.oddlot.ledger.data.Member
//import io.oddlot.ledger.data.Tab
//import io.oddlot.ledger.utils.Utils
//import io.oddlot.ledger.parcelables.TransactionParcelable
//import io.oddlot.ledger.data.Transaction
//import io.oddlot.ledger.parcelables.TabParcelable
//import kotlinx.android.synthetic.main.activity_edit_transaction.amountPaid
//import kotlinx.android.synthetic.main.activity_edit_transaction.datePicker
//import kotlinx.android.synthetic.main.activity_edit_transaction.editDescription
//import kotlinx.android.synthetic.main.activity_edit_transaction.paidBySpinner
//import kotlinx.android.synthetic.main.activity_edit_transaction.tabSpinner
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers.IO
//import kotlinx.coroutines.Dispatchers.Main
//import kotlinx.coroutines.launch
//import kotlin.concurrent.thread
//
//class EditTransactionActivity : AppCompatActivity() {
//    private val TAG = "EDIT_TRANSACTION_ACTIVITY"
//    private lateinit var txnDateString: String // "2019-10-14"
//    private lateinit var txnParcelable: TransactionParcelable
//    private lateinit var tabParcelable: TabParcelable
//    private lateinit var tab: Tab
//    private lateinit var tabs: List<Tab>
//    private lateinit var txn: Transaction
//    private lateinit var payee: Member
//    private lateinit var payer: String
//    private lateinit var members: List<Member>
//    private lateinit var username: String
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_edit_transaction)
//
//        setSupportActionBar(findViewById(R.id.toolbar))
//        supportActionBar?.apply {
//            title = "Edit Transaction"
//            setDisplayShowHomeEnabled(true)
//            setDisplayHomeAsUpEnabled(true)
//        }
//
//        // 2. parcelables
//        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
//        txnParcelable = intent.getParcelableExtra("TXN_PARCELABLE") as TransactionParcelable
//        txnDateString = Utils.dateStringFromMillis(txnParcelable.date, "yyyy/MM/dd")
//
//        username = prefs.getString("USERNAME", "Unknown")!!
//        payer = if (txnParcelable.amount > 0.0) username else tabParcelable.name
//
//        val mItemAmount = if (txnParcelable.amount > 0.0) txnParcelable.amount else txnParcelable.amount * -1.0
//
//        val txnDescription = txnParcelable.description
//        val tabId = txnParcelable.tabId
//        val txnId = txnParcelable.itemId
//        var txnDate = txnParcelable.date // "2019-10-14"
//
//        val dateView = findViewById<TextView>(R.id.datePicker).apply {
//            text = Utils.dateStringFromMillis(txnDate).replace("-", "/")
//        }
//
//        amountPaid.text = SpannableStringBuilder(if (mItemAmount > 0.0) mItemAmount.toString() else (mItemAmount * -1.0).toString())
//        editDescription.text = SpannableStringBuilder(txnDescription)
//
//        datePicker.setOnClickListener {
//            DatePickerDialog(this).apply {
//                this.updateDate(txnDateString.slice(0..3).toInt(), txnDateString.slice(5..6).toInt() - 1, txnDateString.slice(8..9).toInt())
//                this.setOnDateSetListener { view, year, month, day ->
//                    // Get month and day string representations
//                    var month = (month + 1).toString()
//                    var day = day.toString()
//
//                    // Zero pad month and day
//                    if (month.length < 2) month = "0" + month
//                    if (day.length < 2) day = "0" + day
//
//                    val dateStr = "$year/$month/$day".also {
//                        dateView.text = it
//                        txnDateString = it
//                    }
//
//                    txnParcelable.date = Utils.millisFromDateString(dateStr.replace("/", "-"))
//                    txnDate = Utils.millisFromDateString(dateStr.replace("/", "-"))
//                }
//                this.show()
//            }
//        }
//
//        CoroutineScope(IO).launch {
//            tab = db.tabDao().get(txnParcelable.tabId)
//            txn = db.itemDao().get(txnParcelable.itemId)
//            members = db.memberDao().getMembersByTabId(txn.tabId)
//
//            /*
//            Tabs spinner
//             */
//            tabs = db.tabDao().getAll()
//            tabSpinner.adapter = ArrayAdapter<String>(
//                this@EditTransactionActivity,
//                android.R.layout.simple_spinner_dropdown_item,
//                tabs.map { tab -> tab.name }
//            )
//            tabSpinner.setSelection(tabs.indexOf(db.tabDao().get(tabParcelable.id)))
//            tabSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
//                override fun onNothingSelected(parent: AdapterView<*>?) {
//                    TODO("Not yet implemented")
//                }
//
//                override fun onItemSelected(
//                    parent: AdapterView<*>?,
//                    view: View?,
//                    position: Int,
//                    id: Long
//                ) {
//                    CoroutineScope(IO).launch {
//                        tab = tabs[position]
//
//                        CoroutineScope(Main).launch {
//                            paidBySpinner.adapter = ArrayAdapter<String>(
//                                this@EditTransactionActivity,  android.R.layout.simple_spinner_dropdown_item, listOf(username, tab.name)
//                            )
//                        }
//                    }
//                }
//            }
//
//            /*
//            Paid by spinner
//             */
//            ArrayAdapter(
//                this@EditTransactionActivity,
//                android.R.layout.simple_spinner_dropdown_item,
//                listOf(username, tab.name)
//            ).also {
//                paidBySpinner.adapter = it
//
//                // Set Saved Type Selection
//                Log.d(TAG, txn.amount.toString() + username + payer)
//
//            }
//
//            paidBySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
//                override fun onNothingSelected(parent: AdapterView<*>?) {
//                    TODO("Not yet implemented")
//                }
//
//                override fun onItemSelected(
//                    parent: AdapterView<*>?,
//                    view: View?,
//                    position: Int,
//                    id: Long
//                ) {
//                    Log.d(TAG, "paid by spinner selected")
//                    payer = if (position == 0) username else tab.name
//                }
//
//            }
//
//            paidBySpinner.setSelection(if (payer == username) 0 else 1)
//        }
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        val inflater: MenuInflater = menuInflater
//
//        menu!!.add("").apply { // Add tick submit icon
//            icon = getDrawable(R.drawable.ic_check_white_24dp)
//            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
//        }
//
//        inflater.inflate(R.menu.ticket_overflow, menu)
//
//        return super.onCreateOptionsMenu(menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            android.R.id.home -> {
//                val intent = intent.putExtra("TAB_PARCELABLE", tabParcelable)
//                NavUtils.navigateUpTo(this, intent)
//
//                true
//            }
//            0 -> {
//                var txnAmount = amountPaid.text.toString().toDouble() // Positive amount if "Debit"
//                if (payer != username) txnAmount *= -1.0 // Negative if paid to user
//
//                val updateTxn = Transaction(txn.id, tab.id!!, txnAmount, editDescription.text.toString(), txn.date)
//
//                thread {
//                    db.itemDao().insert(updateTxn)
//                }
//
//                // Redirect to Tab Activity
//                Intent(this, IndividualTabActivity::class.java).apply {
//                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // finish all activities on top of individual tab activity
//
//                    putExtra("TAB_PARCELABLE", tabParcelable)
//                    startActivity(this)
//                }
//
//                finish()
//                true
//            }
//            else -> {
//                true
//            }
//        }
//    }
//
//}