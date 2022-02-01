package io.oddlot.opentab.presentation.tab

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.text.*
import android.util.Log
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.robinhood.ticker.TickerUtils
import io.oddlot.opentab.PreferenceKey
import io.oddlot.opentab.R
import io.oddlot.opentab.RequestCode
import io.oddlot.opentab.presentation.adapters.TransactionAdapter
import io.oddlot.opentab.presentation.main.MainActivity
import io.oddlot.opentab.utils.StringUtils
import io.oddlot.opentab.data.*
import io.oddlot.opentab.db
import io.oddlot.opentab.parcelables.TabParcelable
import io.oddlot.opentab.presentation.transaction.TransactionActivity
import io.oddlot.opentab.presentation.transaction.PaymentActivity
import io.oddlot.opentab.utils.basicEditText
import io.oddlot.opentab.utils.round
import io.oddlot.opentab.utils.commatize
import kotlinx.android.synthetic.main.activity_tab.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

class TabActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName
    private lateinit var viewModel: TabViewModel

    private var mTabBalance = 0.0
    private var transactions: MutableList<Transaction> = mutableListOf()
    private lateinit var tabParcelable: TabParcelable
    private lateinit var tab: Tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab)

        /*
        Initialize data and UI
         */
        CoroutineScope(IO).launch {
            initializeDataObjects()

            withContext(Main) {
                val statusBar = window.decorView.findViewById<View>(android.R.id.statusBarBackground)
//                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

                setSupportActionBar(findViewById(R.id.toolbar))
//                supportActionBar?.title = tabParcelable.name
                supportActionBar?.setDisplayShowHomeEnabled(true)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)

                populateTabData(tab)
                loadTransactionsRecyclerView(transactions)
            }
        }

        newTransactionFab.setOnClickListener { fabListener("click") }
        newTransactionFab.setOnLongClickListener { fabListener("longClick") }
    }

    override fun onRestart() {
        super.onRestart()

        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
//        supportActionBar?.title = tabParcelable.name
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
            RequestCode.READ_EXTERNAL_STORAGE ->  launchRestoreRequest(data)
            RequestCode.WRITE_EXTERNAL_STORAGE -> {
                data!!.putExtra(
                    "FILENAME",
                    "${ tabParcelable.name }_${ StringUtils.dateStringFromMillis(Date().time, "yyyyMMdd") }"
                )

                writeExportFile(data)
            }
            RequestCode.CLOSE_TAB -> {
                writeExportFile(data!!)

                // Delete items
                val deleteThread = thread {
                    val tabItems = db.transactionDao().getTransactionsByTabId(tabParcelable.id)
                    for (item in tabItems) db.transactionDao().deleteItemById(item.id!!)
                    db.tabDao().updateTabBalance(tabParcelable.id, 0.0)
                }

                deleteThread.join()

                mTabBalance = 0.0

                runOnUiThread {
                    // Reset views
                    tabBalance.apply {
                        setCharacterLists(TickerUtils.provideNumberList())
                        animationInterpolator = OvershootInterpolator()
                        animationDuration = 800
                    }
                    loadTransactionsRecyclerView(mutableListOf())
                }
            }
            RequestCode.CREATE_DOCUMENT -> writeExportFile(data!!)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.tab_overflow, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> false
            R.id.menu_share_tab -> {
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"

                    val userName = PreferenceManager.getDefaultSharedPreferences(this@TabActivity).getString(
                        PreferenceKey.USER_NAME, "NONAME")
                    val balanceSummary = if (mTabBalance > 0f) "${tab.name} owes $userName" else if (mTabBalance < 0.0) "$userName owes ${tab.name}" else resources.getString(R.string.flat_balance_primary)
                    val adjustedBalance = if (mTabBalance > 0f) {
                        "${tab.currency} $mTabBalance"
                    } else if (mTabBalance < 0f) {
                        "${tab.currency} ${mTabBalance * -1f}"
                    } else ""

                    var shareText = "<b>$balanceSummary  $adjustedBalance\n<b><br><br><u>Recent Transactions:</u><br>"
                    transactions.forEach { shareText += (it.toString() + "<br>") }
                    val shareSpannable = HtmlCompat.fromHtml(shareText, 0)

                    putExtra(Intent.EXTRA_TEXT, shareSpannable.toString())
                }
                startActivity(Intent.createChooser(i, "Share with"))
                true
            }
            R.id.menu_export_to_csv -> exportAndLaunchContentProvider()
            R.id.menu_change_currency -> {
                val builder = MaterialAlertDialogBuilder(this).apply {
                    val currencies = resources.getStringArray(R.array.currencyEntries)
                    val ccySymbols = resources.getStringArray(R.array.currencySymbols)
                    val ccyValues = resources.getStringArray(R.array.currencyValues)

                    setItems(currencies) { dialog, which ->
                        thread {
                            Looper.prepare()
                            val selectedCurrency = ccyValues[which]
                            Log.d(TAG, selectedCurrency.toString())
                            db.tabDao().setCurrency(tabParcelable.id, selectedCurrency)
                            tab.currency = selectedCurrency

                            val newParcelable = TabParcelable(tab.id!!, tab.name, tab.currency)

                            val intent = Intent(this@TabActivity, TabActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra("TAB_PARCELABLE", newParcelable)
                                .putExtra("NEW_TASK_ON_BACK", true)

                            startActivity(intent)
                        }
                    }
                }

                builder.show()

                true
            }
            R.id.menu_rename_tab -> {
                val tabNameInput = basicEditText(this).apply {
//                    background = null
                    gravity = Gravity.START
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(60, 50, 60, 0)
                    }
                    showSoftInputOnFocus = true
                    text = SpannableStringBuilder(tab.name)
                    textSize = 18f
                    typeface = ResourcesCompat.getFont(context, R.font.quicksand)
                }
                val container = FrameLayout(this)
                container.addView(tabNameInput)

                val builder = MaterialAlertDialogBuilder(this).apply {
                    setView(container)
                    setTitle("Rename Tab")
                    setPositiveButton("OK") { dialog, which ->
                        try {
                            val inputText = tabNameInput.text
                            if (tabNameInput.text.isBlank() or (inputText.length > 15))
                                throw IllegalArgumentException("Name is missing or too long")
                            else {
                                val newTab = Tab(tabParcelable.id, tabNameInput.text.trim().toString(), 0.0, tabParcelable.currency)
                                thread {
                                    db.tabDao().updateTab(newTab)

                                    val intent = Intent(this@TabActivity, TabActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                                        putExtra("NEW_TASK_ON_BACK", true)
                                        putExtra("TAB_PARCELABLE", TabParcelable(tab.id!!, newTab.name, tab.currency))
                                    }

                                    startActivity(intent)
                                }
                                Toast.makeText(applicationContext,"Tab renamed \"${newTab.name}\"", Toast.LENGTH_LONG)
                                    .show()
                            }
                        } catch (e: IllegalArgumentException) {
                            if (tabNameInput.text.isBlank())
                                Toast.makeText(applicationContext, "Name is required", Toast.LENGTH_LONG)
                                    .show()
                            else
                                Toast.makeText(applicationContext, "Tab name must be 15 characters or less", Toast.LENGTH_LONG)
                                    .show()
                        }
                    }
                    setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
                }

                builder.show()

                true
            }
            R.id.menu_delete_tab -> {
                val dialog = MaterialAlertDialogBuilder(this).apply {
                    setTitle("Are you sure want to delete this tab?")
                    setPositiveButton("OK") { dialog, which ->
                        CoroutineScope(IO).launch {
                            db.tabDao().deleteTabById(tab.id!!)
                        }

                        // Return to main activity
                        Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(this)
                        }
                    }
//                    setNegativeButton("No, just Delete") { dialog, which ->
//                        try {
//                            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//                                // Insert closing transaction
//                                thread {
//                                    db.transactionDao().insert(
//                                        Transaction(null, tabParcelable.id, tab.balance * -1.0, "Closing", Date().time)
//                                    )
//                                }
//
//                                type = "text/csv"
//                                putExtra(Intent.EXTRA_TITLE, "${tabParcelable.name} (closed).csv")
//
//                                // Invoke onActivityResult()
//                                startActivityForResult(this, RequestCode.CLOSE_TAB)
//                            }
//                        }
//                        catch (e: IllegalArgumentException) {
//                            Toast.makeText(context, "Name is required", Toast.LENGTH_LONG).show()
//                        }
//                    }
                }
                dialog.show()
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

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Private methods
     */
    private fun initializeDataObjects() {
        tabParcelable = intent.getParcelableExtra("TAB_PARCELABLE") as TabParcelable
        viewModel = ViewModelProvider(this).get(TabViewModel::class.java)

        tab = db.tabDao().getTabById(tabParcelable.id)
        transactions = db.transactionDao()
            .getTransactionsByTabId(tabParcelable.id)
            .toMutableList()
        transactions.sortDescending()

        for (item in transactions) {
            mTabBalance += item.amount
        }

        db.tabDao().updateTabBalance(tabParcelable.id, mTabBalance)
        tab = db.tabDao().getTabById(tabParcelable.id)
    }

    private fun fabListener(type: String): Boolean {
        return when (type) {
            "click" -> {
                MaterialAlertDialogBuilder(this).apply {
                    val arr = arrayOf("Debt", "Payment")

                    setTitle("Create new")
                    setItems(arr) { dialog, pos ->
                        if (pos == 0) {
                            val intent = Intent(this@TabActivity, TransactionActivity::class.java).apply {
                                putExtra("TAB_PARCELABLE", tabParcelable)
                            }

                            val options = ActivityOptions.makeCustomAnimation(this@TabActivity, R.anim.enter_from_right, R.anim.exit_static)
                                .toBundle()
                            startActivity(intent, options)
                        } else {
                            val intent = Intent(this@TabActivity, PaymentActivity::class.java).apply {
                                putExtra("TAB_PARCELABLE", tabParcelable)
                            }

                            val options = ActivityOptions.makeCustomAnimation(this@TabActivity, R.anim.enter_from_right, R.anim.exit_static)
                                .toBundle()
                            startActivity(intent, options)

                            true
                        }
                    }
                }.show()

                true
            }
            "longClick" -> {
                val intent = Intent(this@TabActivity, PaymentActivity::class.java).apply {
                    putExtra("TAB_PARCELABLE", tabParcelable)
                }

                val options = ActivityOptions.makeCustomAnimation(this@TabActivity, R.anim.enter_from_right, R.anim.exit_static)
                    .toBundle()
                startActivity(intent, options)

                true
            }
            else -> false
        }
    }

    private fun exportAndLaunchContentProvider(): Boolean {
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
                "${ tabParcelable.name }_${ StringUtils.dateStringFromMillis(Date().time, "yyyyMMdd") }.csv"
            )
            // Launch Content Provider
            startActivityForResult(this, RequestCode.CREATE_DOCUMENT) // invokes onActivityResult()
        }
//                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
//                    startActivityForResult(this, reqCodes.indexOf("WRITE_EXTERNAL_STORAGE"))
//                }

        return true
    }

    private fun loadTransactionsRecyclerView(transactions: MutableList<Transaction>) {
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        transactionsRecyclerView.adapter = TransactionAdapter(transactions, mTabBalance)
    }

    private fun populateTabData(tab: Tab) {
        tabName.text = if (mTabBalance > 0.0) "${tab.name}" else if (mTabBalance < 0.0) "${tab.name}" else "${tab.name}"
        balanceSummary.text = if (mTabBalance > 0.0) " owes you" else if (mTabBalance < 0.0) " is owed" else "is all square! "
//        balanceSummary.text = if (mTabBalance > 0.0) "${tab.name} owes you" else if (mTabBalance < 0.0) "${tab.name} is owed" else resources.getString(R.string.flat_balance_primary)
//        tvTabCurrency.text = ""
//        tvTabCurrency.text = tab.currency
        tvCurrencySymbol.text = Currency
            .getInstance(tab.currency)
            .getSymbol(Locale.CHINA)

        tabBalance.apply {
            animationDuration = 800
            animationInterpolator = OvershootInterpolator()
//            typeface = ResourcesCompat.getFont(context, R.font.quicksand)
            setCharacterLists(TickerUtils.provideNumberList())
            text = if (tab.balance > 0.0) "${tab.balance.commatize()}" else if (tab.balance < 0.0) "${(tab.balance * -1.0).commatize()}" else "0.0"
        }

//        // Set tab balance view color
//        when {
//            tab.balance < 0.0 -> getColor(R.color.Watermelon).apply {
//                tabBalance.setTextColor(this)
//            }
//            tab.balance > 0.0 -> getColor(R.color.BrightTeal).apply {
//                tabBalance.setTextColor(this)
//            }
//            else -> null
//        }
    }

    private fun writeExportFile(data: Intent): OutputStream? {
        val output = contentResolver.openOutputStream(data.data!!, "w")

        output.use {
//            it!!.write("${ pTab.name }\n".toByteArray()) // Tab Name
            it!!.write("Date,Tab,Amount,Description\n".toByteArray()) // Headers
            val transactionsSorted = transactions.toMutableList().apply { sort() }

            for (i in 0 until transactionsSorted.size) {
                var mItemDescription = ""
                val item = transactionsSorted[i]
                val mItemDateString = StringUtils.dateStringFromMillis(item.date)
                item.description?.apply {
                    mItemDescription = '"' + this + '"'
                }
                val mItemAmount = item.amount.toString()
                it.write("$mItemDateString,${tabParcelable.name},$mItemAmount,$mItemDescription\n".toByteArray()) // Data
            }
            it.close()
        }

        Toast.makeText(this, "Exported", Toast.LENGTH_SHORT).show()

        return output
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

                    val txn = Transaction(
                        null,
                        tabId = tabParcelable.id,
                        amount = rowItem[2].toDouble(),
                        description = rowItem[1].removeSurrounding("\""),
                        date = StringUtils.millisFromDateString(rowItem[0])
                    )
                    db.transactionDao().insert(txn)

                    Thread.sleep(50) // Update tab balance view every 50 milliseconds
                    mTabBalance += txn.amount
                    var viewBalance = if (mTabBalance >= 0.0) mTabBalance else mTabBalance * -1.0
                    runOnUiThread {
                        tabBalance.text = viewBalance.round(2).toString()
                    }
                }

                transactions = db.transactionDao().getTransactionsByTabId(tabParcelable.id).toMutableList()
                transactions.sortDescending()

                db.tabDao().updateTabBalance(tabParcelable.id, mTabBalance)
                tab = db.tabDao().getTabById(tabParcelable.id)

                runOnUiThread {
                    Toast.makeText(this, "Transactions restored", Toast.LENGTH_SHORT).show()
                    populateTabData(tab)
                    loadTransactionsRecyclerView(transactions)
                }
                it.close()
            }
        }
    }
}