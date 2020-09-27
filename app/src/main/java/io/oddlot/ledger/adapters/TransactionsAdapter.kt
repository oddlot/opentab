package io.oddlot.ledger.adapters

import android.app.Activity
import android.content.Intent
import android.os.Looper
import android.util.Log
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
import io.oddlot.ledger.utils.commatize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
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
        val view = inflater.inflate(R.layout.layout_solo_transaction_row, parent, false)

        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val txn = transactions[position]
        val txnTabId = txn.tabId
        val txnId = txn.id
        val txnAmount = txn.amount
        val txnDateInMillis = txn.date

        val txnDateView = holder.view.findViewById<TextView>(R.id.txnDateView)
        val txnDescriptionView = holder.view.findViewById<TextView>(R.id.etDescription)
        val txnAmountView = holder.view.findViewById<TextView>(R.id.amountPaid)
        val txnBalanceView = holder.view.findViewById<TextView>(R.id.dynamicTabBalance)

        txnBalanceView.apply {
            val balance = mComputedBalances[position] /* set to cached balance if one exists*/ ?: mComputedBalance.also {
                mLastAmount = txnAmount.also {
                    Log.d(TAG, it.toString())
                }
                mComputedBalance -= mLastAmount
                mComputedBalances[position] = it // memoize
            }

            text = balance.commatize()
        }

        if (position == 0) {
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.topMargin = 25
            holder.view.layoutParams = params
        }

        val paidBy = holder.view.findViewById<TextView>(R.id.paidBy)

        mLastAmount = txnAmount

        // Transaction amount view
        txnAmountView.apply {
//            this.setCharacterLists(TickerUtils.provideNumberList())
//            animationInterpolator = OvershootInterpolator()
//            animationDuration = 800
//            typeface = ResourcesCompat.getFont(context, R.font.abel) // Works
////            typeface = Typeface.createFromAsset(context.assets, "fonts/AbelRegular.ttf") // Doesn't work

            val amount = txnAmount.round(2)

            if (amount < 0.0) {
                text = NumberFormat.getNumberInstance(Locale.getDefault()).format(amount * -1.0)
                paidBy.text = "You were paid"
                setTextColor(ContextCompat.getColor(this.context, R.color.Watermelon))

                CoroutineScope(IO).launch { paidBy.text = "${db.tabDao().tabById(txnTabId).name} paid" }
            }
            else {
                text = "+" + NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)
                paidBy.text = "You paid"
                setTextColor(ContextCompat.getColor(this.context, R.color.BrightTeal))
            }
        }

        txnDateView.text = StringUtils.dateStringFromMillis(txnDateInMillis, "MM/dd")
//        itemDateView.text = itemDate.slice(5..9).replace("-", "/")
//        txnDescriptionView.text = txnDescription

        txn.description?.let {
            if (it.length > 0) txnDescriptionView.text = it
        }

        holder.view.setOnClickListener {
            val txnDateTime = Date(txnDateInMillis)
            val txnDateString = SimpleDateFormat("yyyy-MM-dd").format(txnDateTime)
            val txnParcelable = TransactionParcelable(txnTabId, txnId!!, txnAmount, txn.description ?: "", txnDateInMillis)

            thread {
                val tab = db.tabDao().tabById(txn.tabId)
                tabParcelable = TabParcelable(tab.id!!, tab.name, tab.currency)

                val intent = Intent(it.context, TransactionActivity::class.java)
                intent.putExtra("TXN_PARCELABLE", txnParcelable)
                intent.putExtra("TAB_PARCELABLE", tabParcelable)

                // 3. Start edit transaction activity
                startActivity(it.context, intent, null)
            }
        }

        // Local
        holder.view.setOnLongClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Delete this transaction?")
            builder.setPositiveButton("DELETE") { dialog, which ->
                thread {
                    Looper.prepare()
                    val tab = db.tabDao().tabById(txn.tabId)
                    tabParcelable = TabParcelable(tab.id!!, tab.name, tab.currency)
                    db.transactionDao().deleteItemById(txnId!!)
                    Toast.makeText(it.context, "Transaction deleted", Toast.LENGTH_LONG).show()

                    val intent = Intent(it.context, TabActivity::class.java)
                    intent.putExtra("TAB_PARCELABLE", tabParcelable)
                    intent.putExtra("RESTART_ACTIVITY", true)
                    intent.putExtra("NEW_TASK_ON_BACK", true)

                    startActivity(it.context, intent,null)

                    val activity = it.context as Activity
                    activity.finish()
                }
            }
            builder.setNegativeButton("CANCEL") { dialog, which -> dialog.cancel() }
//            builder.setNeutralButton("Cancel") { dialog, which -> dialog.cancel() }

            val dialog = builder.create()
            dialog.show()

            true
        }


//        // Network
//        holder.view.setOnLongClickListener {
//            val dialog = AlertDialog.Builder(it.context)
//            dialog.setTitle("Delete Item?")
//            dialog.setPositiveButton("OK",
//                DialogInterface.OnClickListener { dialog, which ->
//                    val url = "http://167.99.70.234:8001/api/v1/items/$itemId"
//                    Log.d(TAG, url)
//                    val deleteItem = JsonObjectRequest(
//                        Request.Method.DELETE, url, null,
//                        Response.Listener { response ->
//                            Toast.makeText(it.context, "Item deleted", Toast.LENGTH_LONG).show()
//                            Log.d(TAG, it.context.toString())
//                        },
//                        Response.ErrorListener { error ->
//                            Log.d(TAG, "Error while deleting item")
//                            Toast.makeText(it.context, error.message.toString(), Toast.LENGTH_LONG).show()
//                        }
//                    )
//
//                    val queue = App.getRequestQueue(it.context)
//                    queue!!.add(deleteItem)
//                }
//            )
//            dialog.setNegativeButton("Cancel",
//                DialogInterface.OnClickListener { dialog, which -> dialog.cancel() }
//            )
//            dialog.show()
//            true
//        }

    }

    class TransactionViewHolder(val view: View): RecyclerView.ViewHolder(view)
}
