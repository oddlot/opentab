package io.oddlot.ledger.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
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
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.AllocationAdapter
import io.oddlot.ledger.classes.Allocation
import io.oddlot.ledger.classes.Utils
import io.oddlot.ledger.classes.round
import io.oddlot.ledger.data.Member
import io.oddlot.ledger.data.submit
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.view_models.GroupExpenseViewModel
import io.oddlot.ledger.view_models.GroupItemViewModelFactory
import kotlinx.android.synthetic.main.activity_group_expense.*
import kotlinx.android.synthetic.main.activity_group_expense.paidBySpinner
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ADD GROUP EXPENSE ACTIVITY"

class AddGroupExpenseActivity : AppCompatActivity() {
    private lateinit var mParcelable: TabParcelable
    private lateinit var mGroupExpenseVM: GroupExpenseViewModel
    private lateinit var mMembers: List<Member>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_expense)

        /**
         * Toolbar
         */
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = resources.getString(R.string.actionbar_title_add_group_expense)
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        /**
         * Member variables
         */
        mParcelable = intent.getParcelableExtra("GROUP_TAB_PARCELABLE")!!
//        mGroupExpense = GroupExpense(null, mParcelable.id, 0, 0.0, System.currentTimeMillis(), "", Allocation().serialize())

        /**
         * ViewModel
         */
        mGroupExpenseVM = ViewModelProviders
            .of(this@AddGroupExpenseActivity, GroupItemViewModelFactory(mParcelable.id))
            .get(GroupExpenseViewModel::class.java)
            .also {
                it.allocation.value = Allocation()
            }

        CoroutineScope(IO).launch {
            mMembers = db.memberDao().getMembersByTabId(mParcelable.id)
        }

        /**
         * Observers
         */
        mGroupExpenseVM.amountPaid.observe(this@AddGroupExpenseActivity, Observer {

        })
        mGroupExpenseVM.allocation.observe(this@AddGroupExpenseActivity, Observer {
            Log.d(TAG, "Allocation change observed")

        })

        /**
         * Amount paid
         */
        CoroutineScope(Main).launch {
            amountPaid.addTextChangedListener(object : TextWatcher {
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

//            // Old
//            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
//            val dateString = LocalDate.now().format(formatter)

            // New
            val formatter = SimpleDateFormat("yyyy/MM/dd")
            var date = Date()
            val dateString = formatter.format(date)

            mGroupExpenseVM.date.value = Utils.millisFromDateString(dateString, "yyyy/MM/dd")
            datePicker.text = dateString
            datePicker.setOnClickListener {
                DatePickerDialog(this@AddGroupExpenseActivity).apply {
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

//                        var dateWithHyphens = LocalDate.parse(dialogDateString, formatter) // 2019-11-12
                        date = formatter.parse(dialogDateString)

                        mGroupExpenseVM.date.value = Utils.millisFromDateString(
                            dialogDateString, "yyyy/MM/dd"
                        )
                    }
                    this.show()
                }
            }
        }

        /**
         * Equal Split Button
         */
        equalSplitBtn.setOnClickListener {
            val equalAlloc = mGroupExpenseVM.equalAllocation(mGroupExpenseVM.allocation.value!!.payees.toList())
//            mGroupExpenseVM.allocation.value = equalAlloc

            payeeAllocation.adapter = AllocationAdapter(mGroupExpenseVM, mMembers, mGroupExpenseVM.allocation.value!!)
        }

        equalSplitBtn.setOnLongClickListener {
            Toast.makeText(this, "Split amount equally", Toast.LENGTH_SHORT)
                .show()
            true
        }

        /**
         * Payer Spinner
         */
        CoroutineScope(IO).launch {
            val members: List<Member> = db.memberDao().getMembersByTabId(mParcelable.id)

            val adapter = ArrayAdapter<String>(
                this@AddGroupExpenseActivity,
                android.R.layout.simple_spinner_item,
                members.map { it.name }.toTypedArray()
            )

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            paidBySpinner.adapter = adapter
            paidBySpinner.setSelection(0)
            paidBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, i: Int, p3: Long) {
                    CoroutineScope(Main).launch {
                        mGroupExpenseVM.payer.value = members[i]
                    }
                }

            }

            /**
             * Payees
             */
            withContext(Main) {
                payeeAllocation.adapter = AllocationAdapter(mGroupExpenseVM, members, mGroupExpenseVM.allocation.value!!)
                payeeAllocation.layoutManager = LinearLayoutManager(this@AddGroupExpenseActivity)
            }

//            payeeLabel.setOnClickListener {
//                val payees: Array<String> = mGroupExpenseVM.payees.value!!.map { it.name }.toTypedArray()
//                var selectedPayees = ArrayList<Int>()
//                val checkedPayees = BooleanArray(payees.size)
//
//                val builder = AlertDialog.Builder(this@AddGroupExpenseActivity).apply {
//                    this.setMultiChoiceItems(payees, checkedPayees) { dialog, which, isChecked ->
//                        if (isChecked) {
//                            selectedPayees.add(which)
//                        } else if (selectedPayees.contains(which)) {
//                            selectedPayees.remove(which)
//                        }
//                    }
//                }
//            }

//            /**
//             * Allocation view
//             */
//            withContext(Main) {
//                payeeAllocation.adapter = AllocationAdapter(mGroupExpenseVM, members, mGroupExpenseVM.allocation.value!!)
//                payeeAllocation.layoutManager = LinearLayoutManager(this@AddGroupExpenseActivity)
//            }
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
            0 -> { // Submit
                    val intent = Intent(this@AddGroupExpenseActivity, GroupTabActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("TAB_PARCELABLE", mParcelable)
                    }

                    try {
                        if (mGroupExpenseVM.amountPaid.value == 0.0 || mGroupExpenseVM.unallocated() != 0.0) {
                            throw IllegalStateException("Amount not fully allocated")
                        } else {
                            mGroupExpenseVM.description.value = editDescription.text.toString()

                            CoroutineScope(IO).launch {
                                mGroupExpenseVM
                                    .stage()
                                    .submit()
                                startActivity(intent)
                                finish()
                            }
                        }
                    } catch (e: IllegalStateException) {
                        Toast.makeText(this@AddGroupExpenseActivity, e.message, Toast.LENGTH_LONG).show()
                    }

                true
            }
            android.R.id.home -> {
                NavUtils.navigateUpTo(this, intent.putExtra("TAB_PARCELABLE", mParcelable))

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}