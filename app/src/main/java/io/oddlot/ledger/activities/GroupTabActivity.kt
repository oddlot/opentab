package io.oddlot.ledger.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.GroupExpensesAdapter
import io.oddlot.ledger.*
import io.oddlot.ledger.data.*
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.utils.basicEditText
import io.oddlot.ledger.view_models.GroupTabViewModel
import io.oddlot.ledger.view_models.GroupTabViewModelFactory
import kotlinx.android.synthetic.main.activity_group_tab.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class GroupTabActivity: AppCompatActivity() {
    private val TAG = this::class.java.simpleName

    private lateinit var groupTabParcelable: TabParcelable
    private lateinit var groupTabVM: GroupTabViewModel
    private var mLedger: Ledger? = null
    private var groupMembers: List<Member>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_tab_dark)

        // Member Variables
        groupTabParcelable = intent.getParcelableExtra("TAB_PARCELABLE")!!

        CoroutineScope(IO).launch {
            groupMembers = db.memberDao().getMembersByTabId(groupTabParcelable.id)
        }

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Currency Spinner
        val currencies = resources.getStringArray(R.array.currencies)
        val adapter = ArrayAdapter<String>(
            this,
            R.layout.my_spinner_item,
            currencies
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter
        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                groupTabVM.currency.value = currencies[position]
                CoroutineScope(IO).launch {
                    db.tabDao().setCurrency(groupTabParcelable.id, currencies[position])
                }
            }

        }

        // ViewModels
        groupTabVM = ViewModelProviders
            .of(this@GroupTabActivity, GroupTabViewModelFactory(groupTabParcelable.id))
            .get(GroupTabViewModel::class.java)

        // Observers
        groupTabVM.memberships.observe(this@GroupTabActivity, Observer {
            CoroutineScope(IO).launch {
                groupMembers = db.memberDao().getMembersByTabId(groupTabParcelable.id)
                withContext(Main) {
                    tabMembers.text = groupMembers?.map { it.name }!!.joinToString(", ")
                }
            }
        })

        groupTabVM.items.observe(this@GroupTabActivity, Observer { groupExpenses ->
            mLedger = null // reset ledger
            groupTransactionsRecyclerView.layoutManager = LinearLayoutManager(this@GroupTabActivity)
            groupTransactionsRecyclerView.adapter = GroupExpensesAdapter(groupExpenses).apply {
                notifyDataSetChanged()
            }
        })

        groupTabVM.liveTab.observe(this@GroupTabActivity, Observer {
//            tabCurrency.text = it.currency // textview
        })

        groupTabVM.currency.observe(this@GroupTabActivity, Observer {
            val currencyIndex = resources.getStringArray(R.array.currencies).indexOf(groupTabVM.currency.value)

            currencySpinner.setSelection(currencyIndex)
        })

        /**
         * UI
         */
        tabName.text = groupTabParcelable.name
        CoroutineScope(IO).launch {
            val currency = db.tabDao().get(groupTabParcelable.id).currency
            withContext(Main) {
                groupTabVM.currency.value = currency
            }
        }

        // Tab Summary
        tabName.setOnClickListener {
            CoroutineScope(Main).launch {
                val ledger = mLedger ?: Ledger.getLedger(groupTabParcelable.id).also { mLedger = it }

                val builder = AlertDialog.Builder(this@GroupTabActivity).apply {
                        setTitle("Summary")
                        setView(ledger.asTextView(this@GroupTabActivity))
                    }

                builder.show()
            }
        }

        // Add Group Member
        addGroupMemberIcon.setOnClickListener {
            addMemberDialog(this@GroupTabActivity).also {
                it.requestFocus()
            }

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY) // Works (ish)
//                imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) // Does not work
        }

        // Add New Group Expense
        newTransactionFab.setOnClickListener {
            val intent = Intent(this, GroupTransactionActivity::class.java)

            var extras = Bundle().apply {
                putParcelable("GROUP_TAB_PARCELABLE", groupTabParcelable)
                putInt("GROUP_TAB_ID", groupTabParcelable.id)
            }
            Log.d(TAG, extras.toString())
            intent.putExtras(extras)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        CoroutineScope(Main).cancel()
        CoroutineScope(IO).cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        menu!!.add("Add member").apply {
            icon = getDrawable(R.drawable.ic_group_add_black_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        inflater.inflate(R.menu.group_tab_overflow, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            0 -> {
                Log.d(TAG, "Add member option clicked")
                true
            }
            R.id.menu_edit_members -> {
                Log.d(TAG, R.id.menu_edit_members.toString())
                editMembersDialog()

                true
            }
            R.id.menu_rename_tab -> {
                val tabNameInput = EditText(this).apply {
                    background = null
                    gravity = Gravity.CENTER
                    inputType = InputType.TYPE_CLASS_TEXT
                    inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    showSoftInputOnFocus = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(60, 50, 60, 0)
                    }
                }
                val container = FrameLayout(this)
                container.addView(tabNameInput)

                val builder = AlertDialog.Builder(this).apply {
                    setView(container)
                    setTitle("Rename Tab")
                    setPositiveButton("OK") { dialog, which ->
                        try {
                            val inputText = tabNameInput.text
                            if (tabNameInput.text.isBlank() or (inputText.length > 15))
                                throw IllegalArgumentException("Name is missing or too long")
                            else {
                                thread {
//                                    val tab = Tab(tabParcelable.id, tabNameInput.text.toString())
                                    val tab = db.tabDao().get(groupTabParcelable.id).also {
                                        it.name = tabNameInput.text.toString()
                                    }
                                    db.tabDao().updateTab(tab)

                                    runOnUiThread {
                                        supportActionBar!!.title = tab.name
                                    }
                                }
                                Toast.makeText(context,"Tab name updated", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: IllegalArgumentException) {
                            if (tabNameInput.text.isBlank())
                                Toast.makeText(context, "Name is required", Toast.LENGTH_LONG).show()
                            else
                                Toast.makeText(context, "Tab name must be 15 characters or less", Toast.LENGTH_LONG).show()
                        }
                    }
                    setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                }
                builder.show()

                true
            }
            R.id.menu_export_to_csv -> {
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/csv"
                    putExtra(
                        Intent.EXTRA_TITLE,
                        "${ groupTabParcelable.name }_${ Utils.dateStringFromMillis(Date().time, "yyyyMMdd") }.csv"
                    )

                    startActivityForResult(this, reqCodes.indexOf("CREATE_DOCUMENT")) // invokes onActivityResult()
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == reqCodes.indexOf("WRITE_EXTERNAL_STORAGE")) {
//            if (resultCode == Activity.RESULT_OK) {
//                CoroutineScope(IO).launch {
//                    contentResolver.openOutputStream(data!!.data!!, "w").use {
//                        it!!.write("${ mParcelable.name }\n".toByteArray())
//                        it.write("Date,Description,Amount, Paid By\n".toByteArray())
//                        val sortedList = mGroupItems.await().toMutableList().apply { sort() }
//
//                        for (i in 0 until sortedList.size) {
//                            val item = sortedList[i]
//                            val mItemDateString = Utils.dateStringFromMillis(item.date)
//                            val mItemDescription = '"' + item.description!! + '"'
//                            val mItemAmount = item.amount.toString()
//                            it.write("$mItemDateString,$mItemDescription,$mItemAmount\n".toByteArray())
//                        }
//                        it.close()
//                    }
//                    withContext(Main) {
//                        Toast.makeText(this@GroupTabActivity, "Exported", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
//    }

    private fun addMemberDialog(context: Context): EditText {
        val input = basicEditText(context)
        val container = FrameLayout(context).apply {
            addView(input)
        }

        val builder = AlertDialog.Builder(this).apply {
            setTitle("Add Member")
            setView(container)

            setPositiveButton("ADD") { dialog, which ->
                if (input.text.isEmpty()) {
                    input.error = "Name cannot be blank"
                } else {
                    val name = input.text.toString()

                    CoroutineScope(IO).launch {

                        // Get existing member or initialize new one
                        val member = db.memberDao().getMemberByName(name) ?: Member(null, name)

                        // Get existing member ID or return new member's ID
                        val memberId = member.id ?: db.memberDao().insert(member).toInt() // -1 if member exists

                        // Get existing membership or initialize new one
                        val ms = db.membershipDao().getMembership(groupTabParcelable.id, memberId)
                            ?: Membership(null, groupTabParcelable.id, memberId)

                        // ...insert new membership
                        db.membershipDao().insert(ms)
                    }

                    mLedger = null
                }

                // Hide soft input
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(input.windowToken, 0)
            }

            setNegativeButton("Cancel") { dialog, which ->
                // Hide soft input
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(input.windowToken, 0)
            }
        }

        builder.show()

        return input
    }

    private fun editMembersDialog() {
        val members: Array<String> = groupMembers!!.map { it.name }.toTypedArray()
        val selectedMembers = ArrayList<Int>()
        val checkedMembers = BooleanArray(members.size)

        val builder = AlertDialog.Builder(this).also {
            it.setTitle(R.string.actionbar_overflow_edit_members)
            it.setMultiChoiceItems(members, checkedMembers) { dialog, which, isChecked ->
                if (isChecked) {
                    // If the user checked the item, add it to the selected items
                    selectedMembers.add(which)
                } else if (selectedMembers.contains(which)) {
                    // Else, if the item is already in the array, remove it
                    selectedMembers.remove(Integer.valueOf(which))
                }
            }
            it.setPositiveButton("Remove") { dialog, which ->
                CoroutineScope(IO).launch {
                    selectedMembers.forEach { index ->
                        val memberToDelete = groupMembers!![index]

                        db.membershipDao().delete(groupTabParcelable.id, memberToDelete.id!!)
                    }
                    mLedger = null // reset ledger so it can be recalculated
                }

            }
            it.setNegativeButton("Cancel") { dialog, which ->

            }
        }

        builder.show().also {
            val button = it.getButton(AlertDialog.BUTTON_NEGATIVE)
            // Hide "Edit" button if more than one member is checked
            if (selectedMembers.size > 1) button.visibility = View.GONE else button.visibility = View.VISIBLE
        }
    }

//    private suspend fun getLedger(tabId: Int): Ledger {
//        val ledger = Ledger(tabId) // { Dan=0.0, Thomas=0.0, Brian=0.0, Sherry=0.0, Cecilia=0.0 }
//        var members: List<Member>? = null
//        var groupExpenses: List<GroupExpense>? = null
//
//        withContext(IO) {
//            members = mMembers ?: db.memberDao().getMembersByTabId(tabId)
//            groupExpenses = db.groupItemDao().getGroupExpenseByTabId(tabId)
//        }
//
//        groupExpenses?.forEach { item ->
//            // Get item allocation
//            val dsrMap = item.allocation!!.deserialize() // { 1=28.0, 3=28.0 }
//
//            withContext(IO) {
//                // Update paid amounts
//                val payer = db.memberDao().getMemberById(item.payerId)
//
//                if (payer.name in ledger.keys) {
//                    ledger[payer.name] = ledger[payer.name]!!.minus(item.amount)
//                }
//
//                // Update owed amounts
//                members?.forEach { m ->
//                    ledger[m.name] = ledger[m.name]!!.plus(dsrMap[m.id] ?: 0.0)
//                }
//            }
//        }
//
//        return ledger
//    }
}