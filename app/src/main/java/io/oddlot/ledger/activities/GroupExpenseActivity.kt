package io.oddlot.ledger.activities

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.AllocationAdapter
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.data.Member
import io.oddlot.ledger.data.submit
import io.oddlot.ledger.parcelables.GroupExpenseParcelable
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.view_models.GroupExpenseViewModel
import io.oddlot.ledger.view_models.GroupItemViewModelFactory
import kotlinx.android.synthetic.main.activity_group_expense.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private const val TAG = "GROUP EXPENSE ACTIVITY"

class GroupExpenseActivity : AppCompatActivity() {
    private var groupExpense: GroupExpense? = null
    private lateinit var members: List<Member>
    private var paGroupExpense: GroupExpenseParcelable? = null
    private var paGroupTab: TabParcelable? = null
    private lateinit var viewModel: GroupExpenseViewModel
    private var tabId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_expense)

        // Extras
        tabId = intent.getIntExtra("GROUP_TAB_ID", -1)
        val expenseId = intent.extras?.getInt("GROUP_EXPENSE_ID") ?: -1

        // Parcelable
        paGroupExpense = intent.extras?.getParcelable("GROUP_EXPENSE_PARCELABLE")
        paGroupTab = intent.getParcelableExtra("GROUP_TAB_PARCELABLE")

        setSupportActionBar(findViewById(R.id.toolbar))

        val uiJob = CoroutineScope(IO).launch {
            members = db.memberDao().getMembersByTabId(tabId)
            groupExpense = if (expenseId >= 0) db.groupExpenseDao().getGroupExpenseById(expenseId) else null
            withContext(Main) {
                payeeAllocation.layoutManager = LinearLayoutManager(this@GroupExpenseActivity)
                payeeAllocation.adapter = AllocationAdapter(viewModel, members, viewModel.allocation.value!!)
            }
        }

        if (paGroupExpense == null) {
            // Init fresh Activity for new expense
            supportActionBar.apply {
                title = resources.getString(R.string.actionbar_title_add_group_expense)
            }
            datePicker.text = Utils.dateStringFromMillis(Date().time, "yyyy/MM/dd")
        }

        intent.extras?.getInt("GROUP_EXPENSE_ID")?.let {
            // ViewModel
            CoroutineScope(IO).launch {

                viewModel = ViewModelProviders
                    .of(this@GroupExpenseActivity, GroupItemViewModelFactory(tabId, groupExpense))
                    .get(GroupExpenseViewModel::class.java)
            }.invokeOnCompletion {
                // Initialize Payer Spinner
                val adapter = ArrayAdapter<String>(
                    this@GroupExpenseActivity,
                    android.R.layout.simple_spinner_item,
                    members.map { it.name }.toTypedArray() // Member[] > String[]
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                paidBySpinner.adapter = adapter
                paidBySpinner.setSelection(members.map { it.id }.indexOf(0))

                paGroupExpense?.let {
                    // Bind ViewModel
                    amountPaid.text = SpannableStringBuilder(it.amountPaid.toString())
                    paidBySpinner.setSelection(members.map { it.id }.indexOf(it.payerId))

                    paidBySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {
                        }

                        override fun onItemSelected(adapter: AdapterView<*>?, v: View?, i: Int, p3: Long) {
                            viewModel.payer.value = members[i]
                        }

                    }
                    editDescription.text = SpannableStringBuilder(it.description)
                }
            }
        }

        // Toolbar
        supportActionBar?.apply {
            title = if (groupExpense != null) {
                resources.getString(R.string.actionbar_title_edit_group_expense)
            } else {
                resources.getString(R.string.actionbar_title_add_group_expense)
            }
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        equalSplitBtn.setOnClickListener {
            viewModel.equalAllocation()
            payeeAllocation.adapter = AllocationAdapter(viewModel, members, viewModel.allocation.value!!)
        }
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
            0 -> { // submit
                try {
                    if (viewModel.amountPaid.value == 0.0 || viewModel.unallocated() != 0.0) {
                        throw IllegalStateException("Amount not fully allocated")
                    } else {
                        viewModel.description.value = editDescription.text.toString()

                        CoroutineScope(IO).launch {
                            viewModel
                                .stage(groupExpense!!.id)
                                .submit()

                            val groupTab = db.tabDao().get(groupExpense!!.tabId)
                            val intent = Intent(this@GroupExpenseActivity, GroupTabActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("GROUP_TAB_PARCELABLE", paGroupTab)
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
}