package io.oddlot.ledger.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.lifecycle.ViewModelProviders
import io.oddlot.ledger.R
import io.oddlot.ledger.classes.toDateString
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.parcelables.GroupExpenseParcelable
import io.oddlot.ledger.parcelables.GroupTabParcelable
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.view_models.GroupExpenseViewModel
import io.oddlot.ledger.view_models.GroupItemViewModelFactory
import kotlinx.android.synthetic.main.activity_group_expense.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class GroupExpenseActivity : AppCompatActivity() {
    private var groupExpense: GroupExpense? = null
    private var paGroupExpense: GroupExpenseParcelable? = null
    private var paGroupTab: GroupTabParcelable? = null
    private lateinit var vmGroupExpense: GroupExpenseViewModel
    private var tabId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_expense)

        // Extras
        tabId = intent.extras?.getInt("GROUP_TAB_ID") ?: 0

        // Parcelable
        paGroupExpense = intent.extras?.getParcelable("GROUP_EXPENSE_PARCELABLE")
        paGroupTab = intent.extras?.getParcelable("GROUP_TAB_PARCELABLE")

        // ViewModel
        intent.extras?.getInt("GROUP_EXPENSE_ID")?.let {
            CoroutineScope(IO).launch {
                groupExpense = db.groupExpenseDao().getGroupExpenseById(it)
                vmGroupExpense = ViewModelProviders
                    .of(this@GroupExpenseActivity, GroupItemViewModelFactory(tabId, groupExpense))
                    .get(GroupExpenseViewModel::class.java)
            }.invokeOnCompletion {
                // Bind ViewModel
                Log.d("VM", "COMPLETED")
                datePicker.text = paGroupExpense?.date!!.toDateString("yyyy/MM/dd")
            }
        }

        CoroutineScope(IO).launch {

        }

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = if (groupExpense != null) {
                resources.getString(R.string.actionbar_title_edit_group_expense)
            } else {
                resources.getString(R.string.actionbar_title_add_group_expense)
            }
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        // Bind ViewModel
//        datePicker.text = viewModel.date.value?.toString()
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
//                try {
//                    if (mGroupExpenseVM.amountPaid.value == 0.0 || mGroupExpenseVM.unallocated() != 0.0) {
//                        throw IllegalStateException("Amount not fully allocated")
//                    } else {
//                        mGroupExpenseVM.description.value = itemDescriptionInput.text.toString()
//
//                        CoroutineScope(IO).launch {
//                            mGroupExpenseVM
//                                .stage(mGroupExpense.id) // group item
//                                .submit()
//
//                            val groupTab = db.tabDao().get(mGroupExpense.tabId)
//                            val parcelable = TabParcelable(groupTab.id!!, groupTab.name, groupTab.currency)
//
//                            val intent = Intent(this@EditGroupExpenseActivity, GroupTabActivity::class.java).apply {
//                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                                putExtra("TAB_PARCELABLE", parcelable)
//                            }
//                            startActivity(intent)
//                            finish()
//                        }
//                    }
//                } catch (e: IllegalStateException) {
//                    Toast.makeText(this@EditGroupExpenseActivity, e.message, Toast.LENGTH_LONG).show()
//                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}