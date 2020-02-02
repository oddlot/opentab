package io.oddlot.ledger.adapters

import android.content.Intent
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import io.oddlot.ledger.R
import io.oddlot.ledger.activities.GroupExpenseActivity
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.classes.Utils
import io.oddlot.ledger.classes.deserialize
import io.oddlot.ledger.classes.commatize
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.parcelables.GroupExpenseParcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

class GroupExpensesAdapter(private val groupExpenses: List<GroupExpense>) : RecyclerView.Adapter<GroupExpensesAdapter.GroupItemViewHolder>() {
    val TAG = "GROUP_EXPENSES_ADAPTER"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.group_item_row_dark, parent, false)

        return GroupItemViewHolder(view)
    }

    override fun getItemCount(): Int = groupExpenses.size

    override fun onBindViewHolder(holder: GroupItemViewHolder, position: Int) {
        val groupExpense = groupExpenses[position]

        // UI
        holder.view.findViewById<TextView>(R.id.itemDate).apply {
            text = Utils.dateStringFromMillis(groupExpense.date, "MM/dd")
        }

        holder.view.findViewById<TextView>(R.id.paidBy).apply {
            CoroutineScope(IO).launch {
                val payerName = db.memberDao().getMemberNameById(groupExpense.payerId)

                withContext(Main) {
                    text = payerName + " paid"
                }
            }
        }

        holder.view.findViewById<TextView>(R.id.payees).apply {
            CoroutineScope(IO).launch {
                val payeeList = mutableListOf<String>()

                groupExpense.allocation!!.deserialize().forEach { itemAllocation ->
                    itemAllocation.value?.let {
                        if (it > 0) payeeList.add(db.memberDao().getMemberNameById(itemAllocation.key))
                    }
                }

                withContext(Main) {
                    text = payeeList.joinToString(", ")
                }
            }
        }

        holder.view.findViewById<TextView>(R.id.description).apply {
            if (!groupExpense.description.isNullOrEmpty()) {
                text = groupExpense.description
            }
        }

        holder.view.findViewById<TextView>(R.id.amount).apply {
            text = groupExpense.amount.commatize()
            setTextColor(
                if (groupExpense.amount > 0.0) ContextCompat.getColor(context, R.color.colorWatermelon)
                else (resources.getColor(android.R.color.holo_red_dark))
            )
        }

        // Click
        holder.view.setOnClickListener {
            val intent = Intent(it.context, GroupExpenseActivity::class.java)

            intent.putExtra("GROUP_EXPENSE_ID", groupExpense.id)
            intent.putExtra("GROUP_EXPENSE_PARCELABLE", GroupExpenseParcelable(
                groupExpense.id!!,
                groupExpense.payerId,
                groupExpense.amount,
                groupExpense.date,
                groupExpense.description
            ))
            intent.putExtra("GROUP_TAB_ID", groupExpense.tabId)
            startActivity(it.context, intent, null)
        }

        // Long click
        holder.view.setOnLongClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Are you sure you wish to delete this item?")
            builder.setPositiveButton("Yes") { dialog, which ->
                thread {
                    Looper.prepare()
                    val item = groupExpenses[position]
                    db.groupExpenseDao().deleteGroupItemById(item.id!!)
                    Toast.makeText(it.context, "Item deleted", Toast.LENGTH_LONG).show()
                }
            }
            builder.setNegativeButton("No") { dialog, which -> dialog.cancel() }

            val dialog = builder.create()
            dialog.show()

            true
        }
    }

    class GroupItemViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}