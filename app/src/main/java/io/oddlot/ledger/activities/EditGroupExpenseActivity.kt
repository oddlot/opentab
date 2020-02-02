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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.AllocationAdapter
import io.oddlot.ledger.classes.Utils
import io.oddlot.ledger.classes.deserialize
import io.oddlot.ledger.classes.round
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.data.Member
import io.oddlot.ledger.data.submit
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.view_models.GroupExpenseViewModel
import io.oddlot.ledger.view_models.GroupItemViewModelFactory
import kotlinx.android.synthetic.main.activity_group_expense.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.text.SimpleDateFormat
import java.util.*

class EditGroupExpenseActivity: AppCompatActivity() {
    val TAG = "EDIT_GROUP_ITEM_ACTIVITY"

    private lateinit var mGroupExpense: GroupExpense
    private lateinit var mPayer: Member
    private lateinit var mMembers: Deferred<List<Member>>
    private lateinit var mGroupExpenseVM: GroupExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_expense)

        // Extras
        val expenseId = intent.getIntExtra("GROUP_EXPENSE_ID", 0)
        val tabId = intent.getIntExtra("GROUP_TAB_ID", 0)

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = resources.getString(R.string.actionbar_title_edit_group_expense)
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        /**
         * ViewModel
         */
        CoroutineScope(IO).launch {
            mMembers = async { db.memberDao().getMembersByTabId(tabId) }
            mGroupExpense = db.groupExpenseDao().getGroupExpenseById(expenseId)
            mGroupExpenseVM = ViewModelProviders
                .of(this@EditGroupExpenseActivity, GroupItemViewModelFactory(tabId, mGroupExpense))
                .get(GroupExpenseViewModel::class.java)

            // Observers
            withContext(Main) {
                mGroupExpenseVM.amountPaid.observe(this@EditGroupExpenseActivity, Observer {

                })

                mGroupExpenseVM.allocation.observe(this@EditGroupExpenseActivity, Observer {

                })

                mGroupExpenseVM.payees.observe(this@EditGroupExpenseActivity, Observer {
                    Log.d(TAG + " added/removed payee", it.toString())
                })
            }

            mPayer = db.memberDao().getMemberById(mGroupExpense.payerId)

            // Init allocation (remove?)
            withContext(Main) {
                mGroupExpenseVM.allocation.value = mGroupExpense.allocation.deserialize()
            }

            withContext(Main) {
                paidBySpinner.setSelection(mMembers.await().indexOf(mPayer))
            }
        }

        // Allocation View
        CoroutineScope(IO).launch {
            withContext(Main) {
                val dsrAllocation = mGroupExpense.allocation.deserialize()

                payeeAllocation.layoutManager = LinearLayoutManager(this@EditGroupExpenseActivity)
                payeeAllocation.adapter = AllocationAdapter(mGroupExpenseVM, mMembers.await(), mGroupExpenseVM.allocation.value!!)
                mGroupExpenseVM.allocation.value = dsrAllocation
            }
        }

        // Date, Amount, Description
        CoroutineScope(Main).launch {
            val formatter = SimpleDateFormat("yyyy/MM/dd")
            var date = Date(mGroupExpense.date)
            val dateString = formatter.format(date)

            mGroupExpenseVM.date.value = Utils.millisFromDateString(dateString, "yyyy/MM/dd")
            datePicker.text = dateString
            datePicker.setOnClickListener {
                DatePickerDialog(this@EditGroupExpenseActivity).apply {
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

                        date = formatter.parse(dialogDateString)

                        mGroupExpenseVM.date.value = Utils.millisFromDateString(
                            dialogDateString, "yyyy/MM/dd"
                        )
                    }
                    this.show()
                }
            }

            totalAmount.text = SpannableStringBuilder(mGroupExpense.amount.toString())

            totalAmount.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(amount: Editable?) {
                    if (amount.isNullOrBlank()) {
                        mGroupExpenseVM.amountPaid.value = 0.0
                    } else {
                        val formattedAmount = amount.toString().toDouble().round(2)
                        mGroupExpenseVM.amountPaid.value = formattedAmount
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            })

            itemDescriptionInput.text = SpannableStringBuilder(mGroupExpense.description)
        }

        /**
         * Payer spinner
         */
        CoroutineScope(IO).launch {
            val members = db.memberDao().getMembersByTabId(tabId)

            withContext(Main) {
                val adapter = ArrayAdapter<String>(
                    this@EditGroupExpenseActivity,
                    android.R.layout.simple_spinner_item,
                    members.map { it.name }.toTypedArray()
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                paidBySpinner.adapter = adapter
                paidBySpinner.setSelection(0)
                paidBySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onItemSelected(adapter: AdapterView<*>?, v: View?, i: Int, p3: Long) {
                        CoroutineScope(Main).launch {
                            mGroupExpenseVM.payer.value = mMembers.await()[i]
                        }
                    }

                }
            }
        }

        /**
         * Equal Split Button
         */
        equalSplitBtn.setOnClickListener {
            CoroutineScope(Main).launch {
                val selectedPayees = mGroupExpenseVM.payees.value!!
                val newAlloc = mGroupExpenseVM.equalAllocation(selectedPayees.toList())
                mGroupExpenseVM.allocation.value = newAlloc
                payeeAllocation.adapter = AllocationAdapter(mGroupExpenseVM, mMembers.await(), newAlloc).apply {
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater

        menu!!.add("").apply {
            icon = getDrawable(R.drawable.ic_check_white_24dp).apply {

            }
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        inflater.inflate(R.menu.ticket_overflow, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { // home
                CoroutineScope(IO).launch {
                    val tab = db.tabDao().get(mGroupExpense.tabId)
                    val parcelable = TabParcelable(tab.id!!, tab.name, tab.currency)
                    NavUtils.navigateUpTo(this@EditGroupExpenseActivity, intent.putExtra("TAB_PARCELABLE", parcelable))
                }

                true
            }
            0 -> { // submit
                try {
                    if (mGroupExpenseVM.amountPaid.value == 0.0 || mGroupExpenseVM.unallocated() != 0.0) {
                        throw IllegalStateException("Amount not fully allocated")
                    } else {
                        mGroupExpenseVM.description.value = itemDescriptionInput.text.toString()

                        CoroutineScope(IO).launch {
                            mGroupExpenseVM
                                .stage(mGroupExpense.id) // group item
                                .submit()

                            val groupTab = db.tabDao().get(mGroupExpense.tabId)
                            val parcelable = TabParcelable(groupTab.id!!, groupTab.name, groupTab.currency)

                            val intent = Intent(this@EditGroupExpenseActivity, GroupTabActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("TAB_PARCELABLE", parcelable)
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                } catch (e: IllegalStateException) {
                    Toast.makeText(this@EditGroupExpenseActivity, e.message, Toast.LENGTH_LONG).show()
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}