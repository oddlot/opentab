package io.oddlot.opentab.presentation.adapters

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
import io.oddlot.opentab.R
import io.oddlot.opentab.presentation.transaction.GroupTransactionActivity
import io.oddlot.opentab.db
import io.oddlot.opentab.utils.StringUtils
import io.oddlot.opentab.deserialize
import io.oddlot.opentab.utils.commatize
import io.oddlot.opentab.data.GroupExpense
import io.oddlot.opentab.parcelables.GroupExpenseParcelable
import io.oddlot.opentab.parcelables.TabParcelable
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
        val view = inflater.inflate(R.layout.layout_group_item_row, parent, false)

        return GroupItemViewHolder(view)
    }

    override fun getItemCount(): Int = groupExpenses.size

    override fun onBindViewHolder(holder: GroupItemViewHolder, position: Int) {
        val groupExpense = groupExpenses[position]

        // UI
        holder.view.findViewById<TextView>(R.id.itemDate).apply {
            text = StringUtils.dateStringFromMillis(groupExpense.date, "MM/dd")
        }

        holder.view.findViewById<TextView>(R.id.transactionSummary).apply {
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
                ContextCompat.getColor(
                    context,
                    if (groupExpense.amount > 0.0) R.color.Watermelon else R.color.AppTheme
                )
            )
        }

        // Click
        holder.view.setOnClickListener {
            val intent = Intent(it.context, GroupTransactionActivity::class.java)

            intent.putExtra("GROUP_EXPENSE_ID", groupExpense.id)
            intent.putExtra("GROUP_TAB_ID", groupExpense.tabId)
            intent.putExtra("GROUP_EXPENSE_PARCELABLE", GroupExpenseParcelable(
                groupExpense.id!!,
                groupExpense.payerId,
                groupExpense.amount,
                groupExpense.date,
                groupExpense.description,
                groupExpense.tabId
            ))

            CoroutineScope(IO).launch {
                val tab = db.tabDao().getTabById(groupExpense.tabId)

                withContext(Main) {
                    intent.putExtra("GROUP_TAB_PARCELABLE", TabParcelable(tab.id!!, tab.name, tab.currency))
                    startActivity(it.context, intent, null)
                }
            }
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