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
import io.oddlot.ledger.activities.*
import io.oddlot.ledger.Ledger
import io.oddlot.ledger.utils.round
import io.oddlot.ledger.data.Tab
import io.oddlot.ledger.parcelables.TabParcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*
import kotlin.concurrent.thread

class TabsAdapter(var data: List<Tab>) : RecyclerView.Adapter<TabsAdapter.TabViewHolder>() {
    val TAG = "TABS_ADAPTER"

    override fun getItemCount(): Int {
        return data.size // Local Tab list
//        return data.length() // Network Tab JSONArray
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_tab_row_dark, parent, false)

        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        // Local
        val tab = data[position]
        val tabId = tab.id
        val tabName = tab.name

        /* Bind views */
        val tabNameView = holder.view.findViewById<TextView>(R.id.tabName).apply { text = tab.name }
//        val tabTypeIcon = holder.view.findViewById<ImageView>(R.id.tabTypeIcon).apply {
//            if (tab.isGroup) setImageResource(R.drawable.ic_people_outline_black_24dp)
//        }
        val tabCurrencyView = holder.view.findViewById<TextView>(R.id.tabCurrency).apply {
            text = tab.currency
        }
        val tabBalanceView = holder.view.findViewById<TextView>(R.id.tabBalance)
            .apply {
            text =
                if (tab.balance > 0.0) {
                    tab.balance.round(2).toString()
                }
                else if (tab.balance < 0.0) NumberFormat.getNumberInstance(Locale.getDefault()).format((tab.balance * -1.0).round(2)).also {
                    this.setTextColor(ContextCompat.getColor(holder.view.context, R.color.appTheme))
                }
                else "0.00"
        }

        // Click listener
        holder.view.setOnClickListener {
            val intent = Intent(
                holder.view.context,
                if (tab.isGroup) GroupTabActivity::class.java else IndividualTabActivity::class.java
            )
            intent.putExtra("GROUP_TAB_PARCELABLE", TabParcelable(tab.id!!, tab.name, tab.currency))
            startActivity(holder.view.context, intent, null)
        }

        // Long click listener
        holder.view.setOnLongClickListener {
            val dialog = AlertDialog.Builder(it.context)
            dialog.setTitle("Delete \"$tabName\"?")
            dialog.setPositiveButton("OK") { dialog, which ->

                // Local
                thread {
                    Looper.prepare()
                    db.tabDao().deleteTabById(tabId!!)
                    Toast.makeText(it.context, "$tabName deleted", Toast.LENGTH_LONG).show()
//                    val intent = Intent(it.context, TabsActivity::class.java)
//                    startActivity(it.context, intent,null)

//                    val activity = it.context as Activity
//                    activity.finish()
                }
            }

            dialog.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

            dialog.show()
            true
        }

        /**
         * Tab Balance
         */
        CoroutineScope(IO).launch {
            var tabBal = 0.0
            val ledger = Ledger.getLedger(tab.id!!)

            // Calculate tab balance
            ledger.forEach { memberBalance ->
                memberBalance.value.let {
                    if (it > 0.0) tabBal += it
                }
            }

            // Set tab balance view
            withContext(Main) {
                tabBalanceView.text = tabBal.round(2).toString()
            }
        }
    }

    class TabViewHolder(val view: View): RecyclerView.ViewHolder(view)
}






// Network
//        val tab = data.get(position) as JSONObject
//        val tabId = tab.get("id")
//        val tabName = holder.view.findViewById<TextView>(R.id.tabName).apply {
//            text = tab.get("name").toString()
//        }
//        val tabBalance = holder.view.findViewById<TextView>(R.id.tabBalance).apply {
//            text = tab.get("balance").toString()
//        }
//        var tabCurrency = holder.view.findViewById<TextView>(R.id.tabCurrency).apply {
//            text = tab.get("currency").toString()
//        }






//                    // Network
//                    val url = "http://167.99.70.234:8001/api/v1/tabs/$tabId"
//                    val deleteTab = JsonObjectRequest(
//                        Request.Method.DELETE, url, null,
//                        Response.Listener { response ->
//                            Toast.makeText(it.context, "Tab deleted", Toast.LENGTH_LONG).show()
//                            val intent = Intent(it.context, TabsActivity::class.java)
//                            startActivity(it.context, intent,null)
//                        },
//                        Response.ErrorListener { error ->
//                            Log.d(TAG, "Error while deleting tab")
//                            Toast.makeText(it.context, error.message.toString(), Toast.LENGTH_LONG).show()
//                        }
//                    )
//                    val queue = App.getRequestQueue(it.context)
//                    queue!!.add(deleteTab)