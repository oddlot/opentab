package io.oddlot.ledger.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.AllocationAdapter
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.data.Member
import io.oddlot.ledger.parcelables.GroupExpenseParcelable
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.utils.round
import io.oddlot.ledger.view_models.GroupExpenseViewModel
import io.oddlot.ledger.view_models.GroupExpenseViewModelFactory
import kotlinx.android.synthetic.main.activity_group_expense.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupExpenseActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName

    private lateinit var groupExpense: GroupExpense
    private lateinit var groupMembers: List<Member>
    private var groupExpenseParcelable: GroupExpenseParcelable? = null
    private var groupTabParcelable: TabParcelable? = null
    private lateinit var mViewModel: GroupExpenseViewModel
    private var tabId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_expense)

        groupExpenseParcelable = intent.extras!!.getParcelable("GROUP_EXPENSE_PARCELABLE")
        groupTabParcelable = intent.getParcelableExtra("GROUP_TAB_PARCELABLE")
        tabId = groupTabParcelable!!.id

        CoroutineScope(IO).launch {
            groupMembers = db.memberDao().getMembersByTabId(tabId)
            groupExpense = if (groupExpenseParcelable != null) {
                db.groupExpenseDao().getGroupExpenseById(groupExpenseParcelable?.id!!)
            } else {
                GroupExpense(null, tabId, groupMembers[0].id!!, 0.0)
            }

            // ... then set up UI from view model
            withContext(Main) {
                mViewModel = ViewModelProviders
                    .of(this@GroupExpenseActivity, GroupExpenseViewModelFactory(groupExpense))
                    .get(GroupExpenseViewModel::class.java)

                setUpUi()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater

        menu!!.add("").apply {
            icon = getDrawable(R.drawable.ic_check_white_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        inflater.inflate(R.menu.ticket_overflow, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { // home
                CoroutineScope(IO).launch {
                    val tab = db.tabDao().get(tabId)
                    val parcelable = TabParcelable(tabId, tab.name, tab.currency)
                    NavUtils.navigateUpTo(this@GroupExpenseActivity, intent.putExtra("GROUP_TAB_PARCELABLE", parcelable))
                }

                true
            }
            0 -> { // Submit
                try {
                    if (mViewModel.amountPaid.value == 0.0 || mViewModel.unallocated() != 0.0) {
                        Log.d(TAG, mViewModel.amountPaid.value.toString() + mViewModel.unallocated().toString())
                        throw IllegalStateException("Amount not fully allocated")
                    } else {
                        mViewModel.description.value = editDescription.text.toString()

                        CoroutineScope(IO).launch {
                            groupExpense.apply {
//                               id = 1
                               tabId = tabId
                               payerId = mViewModel.payerId
                               amount = mViewModel.amountPaid.value!!
                               description = mViewModel.description.value
                               date = mViewModel.date
                               allocation = mViewModel.allocation.value!!.serialize()
                            }

                            val groupTab = db.tabDao().get(groupExpense.tabId)
                            val intent = Intent(this@GroupExpenseActivity, GroupTabActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("GROUP_TAB_PARCELABLE", groupTabParcelable)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                } catch (e: IllegalStateException) {
                    Toast.makeText(this@GroupExpenseActivity, e.message, Toast.LENGTH_LONG).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpUi() {
        Log.d(TAG, "Setting up UI")

        // Action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = if (groupExpenseParcelable == null) {
                resources.getString(R.string.actionbar_title_add_group_expense)
            } else {
                resources.getString(R.string.actionbar_title_edit_group_expense)
            }

            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        // Date picker
        var dateString = Utils.dateStringFromMillis(mViewModel.date, "yyyy/MM/dd").also {
            datePicker.text = it
        }
        datePicker.setOnClickListener {
            DatePickerDialog(this@GroupExpenseActivity).apply {
                // Set item date on open
                this.updateDate(dateString.slice(0..3).toInt(), dateString.slice(5..6).toInt() - 1, dateString.slice(8..9).toInt())

                this.setOnDateSetListener { datePicker, year, month, day ->
                    // Set month and day string variables
                    var month = (month + 1).toString()
                    var day = day.toString()

                    // Zero Pad
                    if (month.length < 2) month = "0" + month
                    if (day.length < 2) day = "0" + day

                    val dialogDateString = "$year/$month/$day"

                    val v = it as TextView
                    v.text = dialogDateString

                    mViewModel.date = Utils.millisFromDateString(
                        dialogDateString, "yyyy/MM/dd"
                    )
                }
                this.show()
            }
        }

        payeeAllocation.layoutManager = LinearLayoutManager(this@GroupExpenseActivity)
        payeeAllocation.adapter = AllocationAdapter(mViewModel, groupMembers, mViewModel.allocation.value!!)


        intent.extras?.getInt("GROUP_EXPENSE_ID")?.let {
            // Payer Spinner
            val adapter = ArrayAdapter<String>(
                this@GroupExpenseActivity,
                android.R.layout.simple_spinner_item,
                groupMembers.map { it.name }.toTypedArray() // Member[] > String[]
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            paidBySpinner.adapter = adapter
            paidBySpinner.setSelection(groupMembers.map { it.id }.indexOf(0))

            groupExpenseParcelable?.let {
                // Bind views to view model
                amountPaid.text = SpannableStringBuilder(it.amountPaid.toString())
                paidBySpinner.setSelection(groupMembers.map { it.id }.indexOf(it.payerId))

                paidBySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {
                    }

                    override fun onItemSelected(adapter: AdapterView<*>?, v: View?, i: Int, p3: Long) {
                        mViewModel.payer.value = groupMembers[i]
                    }

                }
                editDescription.text = SpannableStringBuilder(it.description)
            }
        }

        // Amount paid
        amountPaid.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(amount: Editable?) {
                if (amount.isNullOrBlank()) {
                    mViewModel.amountPaid.value = 0.0
                } else {
                    val formattedAmount = amount.toString().toDouble().round(2)
                    mViewModel.amountPaid.value = formattedAmount
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        equalSplitBtn.setOnClickListener {
            Log.d(TAG, mViewModel.payees.toString())
            mViewModel.equalAllocation()
            Log.d(TAG, mViewModel.allocation.value.toString())
            payeeAllocation.adapter = AllocationAdapter(mViewModel, groupMembers, mViewModel.allocation.value!!)
        }
    }
}