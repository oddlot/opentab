package io.oddlot.opentab.ui.transaction

import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.oddlot.opentab.App
import io.oddlot.opentab.PreferenceKey
import io.oddlot.opentab.R
import io.oddlot.opentab.data.Member
import io.oddlot.opentab.data.Tab
import io.oddlot.opentab.utils.StringUtils
import io.oddlot.opentab.data.Transaction
import io.oddlot.opentab.db
import io.oddlot.opentab.parcelables.TabParcelable
import io.oddlot.opentab.parcelables.TransactionParcelable
import io.oddlot.opentab.ui.tab.TabActivity
import io.oddlot.opentab.utils.basicEditText
import kotlinx.android.synthetic.main.activity_create_expense.amountPaid
import kotlinx.android.synthetic.main.activity_create_expense.datePicker
import kotlinx.android.synthetic.main.activity_transaction.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity class for staging and editing transactions.
 *
 * Notes:
 * - Paid By item must be set AFTER setting Tab item or it will be overridden
 */

class DebtActivity : AppCompatActivity() {
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

        userName = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(
            PreferenceKey.USER_NAME, "null")!!
        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
        intent.getParcelableExtra<TransactionParcelable>("TXN_PARCELABLE")?.let {
            txnParcelable = it
            txnAmount = it.amount
            txnDescription = it.description
            itemDate = Date(it.date)
            payerName = if (txnAmount!! > 0.0) userName else tabParcelable.name
        }

        loadUi()
    }

    private fun loadUi() {
        /*
        Toolbar
         */
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = if (txnParcelable == null) "New Debt" else "Edit Debt"
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        /*
        View Model
         */
        viewModel = ViewModelProvider(this).get(TransactionViewModel::class.java).also { vm ->
            txnParcelable?.let { vm.init(it) } // initialize with parcelable data if one exists
        }

        /*
        Credit Debit Toggle
         */
        txnAmount?.let {
//            if (it > 0f) creditSwitch.isChecked = true
            credDebToggle.check (if (it > 0f) {
                R.id.creditButton
            } else {
                R.id.debitButton
            })
        }

        /*
        Date Picker
         */
//            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd") // min API level 26
        val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        val dateString = formatter.format(itemDate)

        datePicker.text = dateString
        datePicker.setOnClickListener {
            var dpd = DatePickerDialog(this@DebtActivity)
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
        Tab, Amount, and Payer views
         */
        CoroutineScope(IO).launch {
            selectedTab = db.tabDao().getTabById(tabParcelable.id)
            tabs = db.tabDao().allTabs()

            CoroutineScope(Main).launch {
                /*
                Tab Spinner
                 */
                val tabIndex = tabs.map { tab -> tab.id }.indexOf(tabParcelable.id)
                tabSpinner.adapter = ArrayAdapter<String>(
                    this@DebtActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    tabs.map { tab -> tab.name }
                )

                tabSpinner.setSelection(tabIndex)
                tabSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener
                {
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
                            selectedPayee = db.memberDao().getMemberByName(userName.trim())!! // remove whitespace from username

                            withContext(Main) {
                                paidBySpinner.adapter = ArrayAdapter<String>(
                                    this@DebtActivity,  android.R.layout.simple_spinner_dropdown_item, listOf(userName, selectedTab.name)
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
                    this@DebtActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(userName, tabParcelable.name)
                )

                txnParcelable?.let {
                    paidBySpinner.setSelection(listOf(userName, tabParcelable.name).indexOf(payerName))
                }

                /*
                CredDeb Toggle
                 */
//                creditSwitch?.setOnCheckedChangeListener { btn, checked ->
//                    if (isTransfer) {
//                        paidByLabel?.text = "From"
//                    } else {
//                        paidByLabel?.text = "Owed To"
//                    }
//                }

                txnDescription?.let { transactionDescription.text = SpannableStringBuilder(it) }
            }
        }

        /**
         * Allocation chips
         */
        allMembersChip.setOnCheckedChangeListener { btn, checked ->
            Log.d(TAG, "All members chip checked")

            allocationChips.forEachIndexed { int, view ->
                if (int > 0) {
                    val chip = view as Chip
                    chip.isChecked = false
                }
            }
        }

        superuserChip?.text = App.getUsername(applicationContext)
        superuserChip.setOnCheckedChangeListener { btn, checked ->
            Log.d("Chip is checked", btn.isChecked.toString())

            if (checked) {
                allMembersChip.isChecked = false
                btn.isChecked = checked
            }
        }

        addMemberChip.setOnClickListener {
            Log.d(TAG, "Add member chip clicked")

            val container = FrameLayout(this).also {
                it.addView(
                    basicEditText(this).apply {
                        hint = "Name"
                    }
                )
            }

            val builder = AlertDialog.Builder(this).apply {
                setTitle("Add New Member")
                setView(container)
                setPositiveButton("Add") { _, _ ->
                    Log.d(TAG, "Added")
                }
                setNegativeButton("Cancel") { _, _ -> Log.d(TAG, "Canceled") }
            }

            val dialog = builder.show()
            dialog.getButton(-1).setOnClickListener {
                Log.d(TAG, "Dialog add button clicked")
                val newChip = Chip(this@DebtActivity).apply {
                    text = "New Member"
                    isCheckable = true
                    isChecked = false
//                    setTextColor(ContextCompat.getColor(this@DebtActivity, R.color.text_checkable))
                    setOnCheckedChangeListener { btn, isChecked ->
                        Log.d("CHECKED STATE = ", isChecked.toString())

                        if (isChecked) {
                            val group = btn.parent as ChipGroup
                            val allChip = group[0] as Chip

                            allChip.isChecked = false
                        }
                    }
                }

                allocationChips.addView(newChip)
                dialog.dismiss()
            }
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

                if (amountPaid.text.isBlank() || amountPaid.text.toString().toFloat() == 0f) {
                    Toast.makeText(applicationContext, "Non-zero amount required", Toast.LENGTH_SHORT).show()

                    return false
                }

                CoroutineScope(IO).launch {
                    payerName = paidBySpinner.selectedItem.toString()
                    txnDescription = transactionDescription.text.toString()

                    var txnAmount = amountPaid.text.toString().toDouble()

                    /* Convert to negative if not paid by owner */
//                    if (payerName != userName && txnAmount > 0.0 ) { // REFACTOR
//                        txnAmount *= -1.0
//                    }

//                    if (!creditSwitch.isChecked) {
//                        txnAmount *= -1.0
//                    }

                    if (credDebToggle.checkedButtonId == R.id.debitButton) {
                        txnAmount *= -1.0
                    }

                    val txn = Transaction(txnParcelable?.id, selectedTab.id ?: tabParcelable.id, txnAmount, txnDescription,
                        StringUtils.millisFromDateString(
                            datePicker.text.toString(), "yyyy/MM/dd"
                        ),
                        isTransfer
                    )
                    db.transactionDao().insert(txn)

                    // Redirect to Tab Activity
                    Intent(this@DebtActivity, TabActivity::class.java).apply {
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