package io.oddlot.ledger.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.text.InputType
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import io.oddlot.ledger.view_models.ItemsViewModel
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.TransactionsAdapter
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.data.*
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.reqCodes
import io.oddlot.ledger.utils.round
import io.oddlot.ledger.utils.commatize
import kotlinx.android.synthetic.main.activity_tab.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.concurrent.thread


class TabActivity : AppCompatActivity() {
    private val TAG = "TAB_ACTIVITY"
    private var mTabBalance = 0.0
    private var tabExpenses: MutableList<Transaction> = mutableListOf()
    private lateinit var pTab: TabParcelable
    private lateinit var tab: Tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab)

        pTab = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable

        val actionBar = supportActionBar.apply {
            title = pTab.name
        }

        CoroutineScope(IO).launch { // Initialize member variables
            tab = db.tabDao().get(pTab.id)
            tabExpenses = db.itemDao()
                .getItemsByTabId(pTab.id)
                .toMutableList()
            tabExpenses.sortDescending()

            /*
            1. Sum up tab amounts
            2. Update Tab balance
            3. Load data in views
             */
            for (item in tabExpenses) {
                mTabBalance += item.amount
            }

            db.tabDao().updateTabBalance(pTab.id, mTabBalance)
            Log.d(TAG, mTabBalance.toString())
            tab = db.tabDao().get(pTab.id)

            withContext(Main) {
                loadTabDataViews(tab)
                loadItemsView(tabExpenses)
            }
        }

        // ...finish setting up UI
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        itemsRecyclerView.layoutManager = LinearLayoutManager(this)

        ViewModelProviders.of(this).get(ItemsViewModel::class.java)
            .getItems().observe(this, Observer {

        })

        addItemFab.setOnClickListener {
            Toast.makeText(this, "Add a new item", Toast.LENGTH_SHORT)
                .show()

            Intent(this, TransactionActivity::class.java).also {
                it.putExtra("TAB_PARCELABLE", pTab)
                startActivity(it)
                // finish()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "Restarting activity")

        pTab = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Resuming activity")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == 0) return

        when (requestCode) {
            reqCodes.indexOf("READ_EXTERNAL_STORAGE") ->  {
                launchRestoreRequest(data)
            }
            reqCodes.indexOf("WRITE_EXTERNAL_STORAGE") -> {
                data!!.putExtra(
                    "FILENAME",
                    "${ pTab.name }_${ Utils.dateStringFromMillis(Date().time, "yyyyMMdd") }"
                )

                writeExportFile(data)
            }
            reqCodes.indexOf("CLOSE_TAB") -> {
                writeExportFile(data)

                // Delete items
                val deleteThread = thread {
                    val tabItems = db.itemDao().getItemsByTabId(pTab.id)
                    for (item in tabItems) db.itemDao().deleteItemById(item.id!!)
                    db.tabDao().updateTabBalance(pTab.id, 0.0)
                }

                deleteThread.join()

                mTabBalance = 0.0

                runOnUiThread {
                    // Reset views
                    tabBalance.text = "0.00"
                    loadItemsView(mutableListOf())
                }
            }
            reqCodes.indexOf("CREATE_DOCUMENT") -> writeExportFile(data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.individual_tab_overflow, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_export_to_csv -> {
                /* Parse tabItems array into CSV */
                val needsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    // Should we show an explanation?

                }
                else {
                    // No explanation needed, we can request the permission.
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PackageManager.PERMISSION_GRANTED)
                }

                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/csv"
                    this.putExtra(
                        Intent.EXTRA_TITLE,
                        "${ pTab.name }_${ Utils.dateStringFromMillis(Date().time, "yyyyMMdd") }.csv"
                    )
                    // Launch Content Provider
                    startActivityForResult(this, reqCodes.indexOf("CREATE_DOCUMENT")) // invokes onActivityResult()
                }
//                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
//                    startActivityForResult(this, reqCodes.indexOf("WRITE_EXTERNAL_STORAGE"))
//                }

                true
            }
            R.id.menu_restore_from_csv -> {
                val needsPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    // Should we show an explanation?
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PackageManager.PERMISSION_GRANTED)

                }

                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "text/csv"

                    // Launch Content Provider
                    startActivityForResult(this, reqCodes.indexOf("READ_EXTERNAL_STORAGE")) // invokes onActivityResult()
                }

                true
            }
//            R.id.menu_settings -> {
//                true
//            }
            R.id.menu_set_currency -> {
                val builder = AlertDialog.Builder(this).apply {
                    setItems(R.array.currencies) { dialog, which ->
                        val currencies = resources.getStringArray(R.array.currencies)
                        thread {
                            Looper.prepare()
                            val selectedCurrency = currencies[which].also {
                                db.tabDao().setCurrency(pTab.id, it)
                                tab.currency = it
                            }
                            runOnUiThread {
                                tabCurrency.text = selectedCurrency
                            }
                        }
                    }
                }
                val dialog = builder.create()
                dialog.show()

                true
            }
            R.id.menu_rename_tab -> {
                val tabNameInput = EditText(this).apply {
                    background = null
                    gravity = Gravity.CENTER
                    inputType = InputType.TYPE_CLASS_TEXT
                    inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(60, 50, 60, 0)
                    }
                    showSoftInputOnFocus = true
                    text = SpannableStringBuilder(tab.name)
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
                                val newTab = Tab(pTab.id, tabNameInput.text.toString(), 0.0)
                                thread {
                                    db.tabDao().updateTab(newTab)

                                    runOnUiThread {
//                                        supportActionBar!!.title = newTab.name
                                        tabName.text = newTab.name
                                    }
                                }
                                Toast.makeText(context,"Tab renamed \"${newTab.name}\"", Toast.LENGTH_LONG)
                                    .show()
                            }
                        } catch (e: IllegalArgumentException) {
                            if (tabNameInput.text.isBlank())
                                Toast.makeText(context, "Name is required", Toast.LENGTH_LONG)
                                    .show()
                            else
                                Toast.makeText(context, "Tab name must be 15 characters or less", Toast.LENGTH_LONG)
                                    .show()
                        }
                    }
                    setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                }
                builder.show()

                true
            }
            R.id.menu_close_tab -> {
                val dialog = AlertDialog.Builder(this).apply {
                    setTitle("Close tab and export to CSV?")
                    setPositiveButton("OK") { dialog, which ->
                        try {
                            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                // Insert closing transaction
                                thread {
                                    db.itemDao().insert(
                                        Transaction(null, pTab.id, tab.balance * -1.0, "Closing", Date().time)
                                    )
                                }

                                type = "text/csv"
                                putExtra(Intent.EXTRA_TITLE, "${pTab.name} (closed).csv")

                                // Invoke onActivityResult()
                                startActivityForResult(this, reqCodes.indexOf("CLOSE_TAB"))
                            }
                        }
                        catch (e: IllegalArgumentException) {
                            Toast.makeText(context, "Name is required", Toast.LENGTH_LONG).show()
                        }
                    }
                    setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                }
                dialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun loadItemsView(expenses: MutableList<Transaction>) {
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsRecyclerView.adapter = TransactionsAdapter(expenses, mTabBalance)
    }

    private fun loadTabDataViews(tab: Tab) {
        tabName.text = if (mTabBalance > 0.0) "${pTab.name} owes you" else if (mTabBalance < 0.0) "You owe ${pTab.name} " else ""
        tabCurrency.text = pTab.currency
        tabBalance.text = if (tab.balance >= 0.0) "${tab.balance.commatize()}" else "${(tab.balance * -1.0).commatize()}"
        if (tab.balance < 0.0) tabBalance.setTextColor(getColor(R.color.appTheme))
    }

    private fun writeExportFile(data: Intent?) {

        // Export to CSV
        contentResolver.openOutputStream(data!!.data!!, "w").use {
            it!!.write("${ pTab.name }\n".toByteArray())
            it.write("Date,Description,Amount\n".toByteArray())
            val sortedList = tabExpenses.toMutableList().apply { sort() }

            for (i in 0 until sortedList.size) {
                var mItemDescription = ""
                val item = sortedList[i]
                val mItemDateString = Utils.dateStringFromMillis(item.date)
                item.description?.apply {
                    mItemDescription = '"' + this + '"'
                }
                val mItemAmount = item.amount.toString()
                it.write("$mItemDateString,$mItemDescription,$mItemAmount\n".toByteArray())
            }
            it.close()
        }

        Toast.makeText(this, "Exported", Toast.LENGTH_SHORT).show()
    }

    private fun launchRestoreRequest(data: Intent?) {
        val sourceTreeUri = data!!.data!! // "content://com.android.providers.downloads.documents/document/35"

        thread {
            Looper.prepare()
            contentResolver.takePersistableUriPermission(sourceTreeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            contentResolver.openInputStream(sourceTreeUri)!!.use {
                val reader = it.reader()
                val lines = reader.readLines()

                for (i in 2 until lines.size) { // Start reading from line 3
                    val rowItem = lines[i]
                        .split(",")
//                                .split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)") // Preserves commas inside quotes

                    val item = Transaction(
                        null,
                        tabId = pTab.id,
                        amount = rowItem[2].toDouble(),
                        description = rowItem[1].removeSurrounding("\""),
                        date = Utils.millisFromDateString(rowItem[0])
                    )
                    db.itemDao().insert(item)

                    Thread.sleep(50) // Update tab balance view every 50 milliseconds
                    mTabBalance += item.amount
                    var viewBalance = if (mTabBalance >= 0.0) mTabBalance else mTabBalance * -1.0
                    runOnUiThread {
                        tabBalance.text = viewBalance.round(2).toString()
                    }
                }

                tabExpenses = db.itemDao().getItemsByTabId(pTab.id).toMutableList()
                tabExpenses.sortDescending()

                db.tabDao().updateTabBalance(pTab.id, mTabBalance)
                tab = db.tabDao().get(pTab.id)

                runOnUiThread {
                    Toast.makeText(this, "Items Restored!", Toast.LENGTH_LONG).show()
                    loadTabDataViews(tab)
                    loadItemsView(tabExpenses)
                }
                it.close()
            }
        }
    }
}