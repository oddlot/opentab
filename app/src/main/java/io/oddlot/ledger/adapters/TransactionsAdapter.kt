package io.oddlot.ledger.adapters

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import io.oddlot.ledger.R
import io.oddlot.ledger.activities.TabActivity
import io.oddlot.ledger.activities.TransactionActivity
import io.oddlot.ledger.parcelables.TransactionParcelable
import io.oddlot.ledger.db
import io.oddlot.ledger.utils.StringUtils
import io.oddlot.ledger.utils.round
import io.oddlot.ledger.data.Transaction
import io.oddlot.ledger.parcelables.TabParcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*
import kotlin.concurrent.thread


class TransactionsAdapter(var transactions: List<Transaction>, startingTabBalance: Double = 0.0) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {
//class ItemsAdapter(var items: JSONArray) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {
    private val TAG = "TRANSACTIONS_ADAPTER"
    private lateinit var tabParcelable: TabParcelable
    private var mLastAmount = 0.0
    private var mComputedBalance = startingTabBalance
    private var mComputedBalances = HashMap<Int, Double>()

    override fun getItemCount(): Int = transactions.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_transaction_row, parent, false)

        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val txn = transactions[position]
        val txnTabId = txn.tabId
        val txnDateInMillis = txn.date

        val txnDateView = holder.view.findViewById<TextView>(R.id.transactionDate)
        val tvSummary = holder.view.findViewById<TextView>(R.id.transactionSummary)
        val txnDescriptionView = holder.view.findViewById<TextView>(R.id.transactionDescription)
        val tvAmount = holder.view.findViewById<TextView>(R.id.amountPaid)
        val tvAggregateBalance = holder.view.findViewById<TextView>(R.id.aggregateTabBalance)

//        tvAggregateBalance?.apply {
//            val balance = mComputedBalances[position] /* set to cached balance if one exists*/ ?: mComputedBalance.also {
//                mLastAmount = txnAmount.also {
//                    Log.d(TAG, it.toString())
//                }
//                mComputedBalance -= mLastAmount
//                mComputedBalances[position] = it // memoize
//            }
//
//            text = balance.commatize()
//        }

        if (position == 0) {
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.topMargin = 25
            holder.view.layoutParams = params
        }

        mLastAmount = txn.amount

        CoroutineScope(IO).launch {
            val tabName = db.tabDao().tabById(txnTabId).name
            val roundedAmount = txn.amount.round(2)

            withContext(Main) {
                if (txn.amount > 0) {
                    tvSummary.text = if (txn.isTransfer) { "You transferred $tabName" } else { "$tabName owes you" }
                    tvAmount.text = "+" + NumberFormat.getNumberInstance(Locale.getDefault()).format(roundedAmount)
                    tvAmount.setTextColor(ContextCompat.getColor(holder.view.context, R.color.BrightTeal))
                } else {
                    tvSummary.text = if (txn.isTransfer) { "$tabName transferred you" } else { "You owe $tabName" }
                    tvAmount.text = NumberFormat.getNumberInstance(Locale.getDefault()).format(roundedAmount * -1.0)
                    tvAmount.setTextColor(ContextCompat.getColor(holder.view.context, R.color.Watermelon))
                }
            }
        }

        txnDateView.text = StringUtils.dateStringFromMillis(txnDateInMillis, "MM/dd")

        txn.description?.let {
            if (it.length > 0) txnDescriptionView.text = it
        }

        holder.view.setOnClickListener { clickListener(it.context, txn) }
        holder.view.setOnLongClickListener { longClickListener(it.context, txn) }
    }

    private fun clickListener(context: Context, txn: Transaction): Boolean {
        val txnParcelable = TransactionParcelable(txn.tabId, txn.id!!, txn.amount, txn.description ?: "", txn.date, if (txn.isTransfer) 1 else 0)

        thread {
            val tab = db.tabDao().tabById(txn.tabId)
            tabParcelable = TabParcelable(tab.id!!, tab.name, tab.currency)

            val intent = Intent(context, TransactionActivity::class.java)
            intent.putExtra("TXN_PARCELABLE", txnParcelable)
            intent.putExtra("TAB_PARCELABLE", tabParcelable)

            startActivity(context, intent, ActivityOptions.makeCustomAnimation(context, R.anim.enter_from_right, R.anim.exit_static).toBundle())
        }

        return true
    }

    private fun longClickListener(context: Context, txn: Transaction): Boolean {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete this transaction?")
        builder.setPositiveButton("DELETE") { dialog, which ->
            thread {
                Looper.prepare()
                val tab = db.tabDao().tabById(txn.tabId)
                tabParcelable = TabParcelable(tab.id!!, tab.name, tab.currency)
                db.transactionDao().deleteItemById(txn.id!!)
                Toast.makeText(context, "Transaction deleted", Toast.LENGTH_LONG).show()

                val intent = Intent(context, TabActivity::class.java)
                intent.putExtra("TAB_PARCELABLE", tabParcelable)
                intent.putExtra("RESTART_ACTIVITY", true)
                intent.putExtra("NEW_TASK_ON_BACK", true)

                startActivity(context, intent,null)

//                    val activity = it.context as Activity
//                    activity.finish()
            }
        }
        builder.setNegativeButton("CANCEL") { dialog, which -> dialog.cancel() }

        val dialog = builder.create()
        dialog.show()

        return true
    }

    class TransactionViewHolder(val view: View): RecyclerView.ViewHolder(view)
}
