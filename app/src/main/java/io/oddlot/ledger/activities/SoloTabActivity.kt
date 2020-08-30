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
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.robinhood.ticker.TickerUtils
import io.oddlot.ledger.view_models.TransactionsViewModel
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.TransactionsAdapter
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.data.*
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.reqCodes
import io.oddlot.ledger.utils.round
import io.oddlot.ledger.utils.commatize
import kotlinx.android.synthetic.main.activity_individual_tab.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.concurrent.thread


class SoloTabActivity : AppCompatActivity() {
    private val TAG = "INDIVIDUAL_TAB_ACTIVITY"
    private var mTabBalance = 0.0
    private var transactions: MutableList<Transaction> = mutableListOf()
    private lateinit var pTab: TabParcelable
    private lateinit var tab: Tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_individual_tab)

        pTab = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable

        val actionBar = supportActionBar.apply {
            title = pTab.name
        }

        CoroutineScope(IO).launch { // Initialize member variables
            tab = db.tabDao().get(pTab.id)
            transactions = db.transactionDao()
                .getTransactionsByTabId(pTab.id)
                .toMutableList()
            transactions.sortDescending()

            /*
            1. Sum up tab amounts
            2. Update Tab balance
            3. Load data in views
             */
            for (item in transactions) {
                mTabBalance += item.amount
            }

            db.tabDao().updateTabBalance(pTab.id, mTabBalance)
            Log.d(TAG, mTabBalance.toString())
            tab = db.tabDao().get(pTab.id)

            withContext(Main) {
                loadTabDataViews(tab)
                loadTransactionsRecyclerView(transactions)
            }
        }

        // ...finish setting up UI
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        ViewModelProviders.of(this).get(TransactionsViewModel::class.java)
            .getItems().observe(this, Observer {

        })

        /*
        New Transaction Fab
         */
        findViewById<FloatingActionButton>(R.id.newTransactionFab).also {
            it.setOnClickListener {
                Toast.makeText(this, "Add transaction", Toast.LENGTH_SHORT)
                    .show()

                Intent(this, SoloTransactionActivity::class.java).also {
                    it.putExtra("TAB_PARCELABLE", pTab)
                    startActivity(it)
                }
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

    override fun onBackPressed() {
        when (intent.extras?.get("NEW_TASK_ON_BACK")) {
            true -> {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Launch new main activity if NEW_TASK_ON_BACK = true
                })
            }
            else -> super.onBackPressed()
        }
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
                    val tabItems = db.transactionDao().getTransactionsByTabId(pTab.id)
                    for (item in tabItems) db.transactionDao().deleteItemById(item.id!!)
                    db.tabDao().updateTabBalance(pTab.id, 0.0)
                }

                deleteThread.join()

                mTabBalance = 0.0

                runOnUiThread {
                    // Reset views
                    tabBalance.apply {
                        setCharacterLists(TickerUtils.provideNumberList())
                        animationInterpolator = OvershootInterpolator()
                        animationDuration = 800
                        tabBalance.typeface = ResourcesCompat.getFont(this@SoloTabActivity, R.font.rajdhani)
                    }
                    loadTransactionsRecyclerView(mutableListOf())
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
//            R.id.menu_restore_from_csv -> {
//                val needsPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
//                if (needsPermission) {
//                    // Should we show an explanation?
//                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PackageManager.PERMISSION_GRANTED)
//
//                }
//
//                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//                    type = "text/csv"
//
//                    // Launch Content Provider
//                    startActivityForResult(this, reqCodes.indexOf("READ_EXTERNAL_STORAGE")) // invokes onActivityResult()
//                }
//
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
//                                        tabName.text = newTab.name
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
            R.id.menu_delete_tab -> {
                val dialog = AlertDialog.Builder(this).apply {
                    setTitle("Close tab and export to CSV?")
                    setPositiveButton("OK") { dialog, which ->
                        try {
                            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                // Insert closing transaction
                                thread {
                                    db.transactionDao().insert(
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
                    setNegativeButton("No, just Delete") { dialog, which ->
                        CoroutineScope(IO).launch {
                            db.tabDao().deleteTabById(tab.id!!)
                        }

                        // Return to main activity
                        Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(this)
                        }

//                        dialog.cancel()
                    }
                }
                dialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun loadTransactionsRecyclerView(transactions: MutableList<Transaction>) {
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        transactionsRecyclerView.adapter = TransactionsAdapter(transactions, mTabBalance)
    }

    private fun loadTabDataViews(tab: Tab) {
        balanceDescriptor.text = if (mTabBalance > 0.0) "${pTab.name} owes you" else if (mTabBalance < 0.0) "${pTab.name} is owed" else resources.getString(R.string.flat_balance_primary)
        tabCurrency.text = pTab.currency
//        tabBalance.text = if (tab.balance >= 0.0) "${tab.balance.commatize()}" else "${(tab.balance * -1.0).commatize()}"

        tabBalance.apply {
            setCharacterLists(TickerUtils.provideNumberList())
            animationInterpolator = OvershootInterpolator()
            animationDuration = 800
            tabBalance.typeface = ResourcesCompat.getFont(this@SoloTabActivity, R.font.rajdhani)
            text = if (tab.balance > 0.0) "${tab.balance.commatize()}" else if (tab.balance < 0.0) "${(tab.balance * -1.0).commatize()}" else "0.0"
        }

        when {
            tab.balance < 0.0 -> getColor(R.color.watermelon).apply {
                tabBalance.setTextColor(this)
                tabCurrency.setTextColor(this)
                Log.d(TAG, tab.balance.toString())
            }
            tab.balance > 0.0 -> getColor(R.color.colorTeal).apply {
                tabBalance.setTextColor(this)
                tabCurrency.setTextColor(this)
                Log.d(TAG, tab.balance.toString())
            }
            else -> null
        }
    }

    private fun writeExportFile(data: Intent?) {
        // Export to CSV
        contentResolver.openOutputStream(data!!.data!!, "w").use {
            it!!.write("${ pTab.name }\n".toByteArray())
            it.write("Date,Description,Amount\n".toByteArray())
            val transactionsSorted = transactions.toMutableList().apply { sort() }

            for (i in 0 until transactionsSorted.size) {
                var mItemDescription = ""
                val item = transactionsSorted[i]
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
                    db.transactionDao().insert(item)

                    Thread.sleep(50) // Update tab balance view every 50 milliseconds
                    mTabBalance += item.amount
                    var viewBalance = if (mTabBalance >= 0.0) mTabBalance else mTabBalance * -1.0
                    runOnUiThread {
                        tabBalance.text = viewBalance.round(2).toString()
                    }
                }

                transactions = db.transactionDao().getTransactionsByTabId(pTab.id).toMutableList()
                transactions.sortDescending()

                db.tabDao().updateTabBalance(pTab.id, mTabBalance)
                tab = db.tabDao().get(pTab.id)

                runOnUiThread {
                    Toast.makeText(this, "Items Restored!", Toast.LENGTH_LONG).show()
                    loadTabDataViews(tab)
                    loadTransactionsRecyclerView(transactions)
                }
                it.close()
            }
        }
    }
}