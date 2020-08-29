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
import io.oddlot.ledger.activities.EditItemActivity
import io.oddlot.ledger.R
import io.oddlot.ledger.activities.TabActivity
import io.oddlot.ledger.parcelables.ItemParcelable
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.utils.Utils
import io.oddlot.ledger.utils.round
import io.oddlot.ledger.data.Expense
import io.oddlot.ledger.parcelables.TabParcelable
import io.oddlot.ledger.utils.commatize
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread


class TransactionAdapter(var expenses: List<Expense>, startingTabBalance: Double = 0.0) : RecyclerView.Adapter<TransactionAdapter.ExpenseViewHolder>() {
//class ItemsAdapter(var items: JSONArray) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {
    private val TAG = "ITEMS_ADAPTER"
    private lateinit var tabParcelable: TabParcelable
    private var mLastAmount = 0.0
    private var mComputedBalance = startingTabBalance
    private var mComputedBalances = HashMap<Int, Double>()

//    override fun getItemCount(): Int = items.length()
    override fun getItemCount(): Int = expenses.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        /*
        1. Get inflater
        2. Inflate view
         */
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_item_row_2, parent, false)

        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        // Item Data
        val item = expenses[position]
        val itemTabId = item.tabId
        val itemId = item.id
        val itemAmount = item.amount
        val itemDescription = item.description
        val itemDateInMillis = item.date

        // View Variables
        val itemDateView = holder.view.findViewById<TextView>(R.id.itemDateView)
        val itemDescriptionView = holder.view.findViewById<TextView>(R.id.editDescription)
        val itemAmountView = holder.view.findViewById<TextView>(R.id.amountPaid)
        val itemBalanceView = holder.view.findViewById<TextView>(R.id.dynamicTabBalance)

        itemBalanceView.apply {
            val balance = mComputedBalances[position] /* set to cached balance if one exists*/ ?: mComputedBalance.also {
                mLastAmount = itemAmount.also {
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

        val whoPaid = holder.view.findViewById<TextView>(R.id.whoPaid)

        if (itemAmount as Double > 0.0) {
//        if (itemJsonObject.get("amount") as Double > 0.0) {
            whoPaid.text = "You paid"
        }
        else {
            thread {
                val tabName = db.tabDao().get(itemTabId).name
                whoPaid.text = "$tabName paid"
            }
        }

        mLastAmount = itemAmount

        // Set Item Amount View Text
        itemAmountView.apply {
            val amount = itemAmount.round(2)
            if (amount < 0.0) {
                text = "+" + NumberFormat.getNumberInstance(Locale.getDefault()).format(amount * -1.0)
                setTextColor(ContextCompat.getColor(this.context, R.color.appTheme))
            }
            else {
                text = NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)
                setTextColor(ContextCompat.getColor(this.context, R.color.colorSecondaryDark))
            }
        }

        itemDateView.text = Utils.dateStringFromMillis(itemDateInMillis, "MM/dd")
//        itemDateView.text = itemDate.slice(5..9).replace("-", "/")
        itemDescriptionView.text = itemDescription

        holder.view.setOnClickListener {
//            val itemDateTime = Instant.ofEpochMilli(itemDateInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
//            val itemDateString = itemDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val itemDateTime = Date(itemDateInMillis)
            val itemDateString = SimpleDateFormat("yyyy-MM-dd").format(itemDateTime)

            val itemParcelable = ItemParcelable(itemTabId, itemId!!, itemAmount, itemDescription, itemDateInMillis)

            thread {
                val tab = db.tabDao().get(item.tabId)
                tabParcelable = TabParcelable(tab.id!!, tab.name, tab.currency)

                val intent = Intent(it.context, EditItemActivity::class.java)
                intent.putExtra("ITEM_PARCELABLE", itemParcelable)
                intent.putExtra("TAB_PARCELABLE", tabParcelable)

                // 3. Start item editor activity
                startActivity(it.context, intent, null)
            }
        }

        // Local
        holder.view.setOnLongClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Delete this item?")
            builder.setPositiveButton("DELETE") { dialog, which ->
                thread {
                    Looper.prepare()
                    val tab = db.tabDao().get(item.tabId)
                    tabParcelable = TabParcelable(tab.id!!, tab.name, tab.currency)
                    db.itemDao().deleteItemById(itemId!!)
                    Toast.makeText(it.context, "Item deleted", Toast.LENGTH_LONG).show()

                    val intent = Intent(it.context, TabActivity::class.java)
                    intent.putExtra("TAB_PARCELABLE", tabParcelable)
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

    class ExpenseViewHolder(val view: View): RecyclerView.ViewHolder(view)
}
