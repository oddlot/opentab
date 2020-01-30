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
import io.oddlot.ledger.activities.EditGroupExpenseActivity
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.classes.Utils
import io.oddlot.ledger.classes.deserialize
import io.oddlot.ledger.classes.commatize
import io.oddlot.ledger.data.GroupExpense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

val TAG = "GROUP_ITEMS_ADAPTER"

class GroupItemsAdapter(private val groupExpenses: List<GroupExpense>) : RecyclerView.Adapter<GroupItemsAdapter.GroupItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.group_item_row_dark, parent, false)

        return GroupItemViewHolder(view)
    }

    override fun getItemCount(): Int = groupExpenses.size

    override fun onBindViewHolder(holder: GroupItemViewHolder, position: Int) {
        val groupItem = groupExpenses[position]

        // UI
        holder.view.findViewById<TextView>(R.id.itemDate).apply {
            text = Utils.dateStringFromMillis(groupItem.date, "MM/dd")
        }

        holder.view.findViewById<TextView>(R.id.paidBy).apply {
            CoroutineScope(IO).launch {
                val payerName = db.memberDao().getMemberNameById(groupItem.payerId)

                withContext(Main) {
                    text = payerName + " paid"
                }
            }
        }

        holder.view.findViewById<TextView>(R.id.payees).apply {
            CoroutineScope(IO).launch {
                val payeeList = mutableListOf<String>()

                groupItem.allocation!!.deserialize().forEach { itemAllocation ->
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
            if (!groupItem.description.isNullOrEmpty()) {
                text = groupItem.description
            }
        }

        holder.view.findViewById<TextView>(R.id.amount).apply {
            text = groupItem.amount.commatize()
            setTextColor(
                if (groupItem.amount > 0.0) ContextCompat.getColor(context, R.color.colorWatermelon)
                else (resources.getColor(android.R.color.holo_red_dark))
            )
        }

        // Click -> Edit Group Item
        holder.view.setOnClickListener {
            val intent = Intent(it.context, EditGroupExpenseActivity::class.java)

            intent.putExtra("GROUP_ITEM_ID", groupItem.id)
            intent.putExtra("GROUP_TAB_ID", groupItem.tabId)
            startActivity(it.context, intent, null)
        }

        // Long click -> Delete Group Item
        holder.view.setOnLongClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setTitle("Are you sure you wish to delete this item?")
            builder.setPositiveButton("Yes") { dialog, which ->
                thread {
                    Looper.prepare()
                    val item = groupExpenses[position]
                    db.groupItemDao().deleteGroupItemById(item.id!!)
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