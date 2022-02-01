package io.oddlot.opentab.presentation.adapters

import android.content.Context
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.oddlot.opentab.PreferenceKey
import io.oddlot.opentab.R
import io.oddlot.opentab.presentation.main.MainActivity
import io.oddlot.opentab.utils.round
import io.oddlot.opentab.data.Tab
import io.oddlot.opentab.db
import io.oddlot.opentab.parcelables.TabParcelable
import io.oddlot.opentab.presentation.tab.TabActivity
import io.oddlot.opentab.utils.commatize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.util.*

class TabAdapter(var data: List<Tab>) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {
    val TAG = this::class.java.simpleName

    override fun getItemCount(): Int {
        return data.size // Local Tab list
//        return data.length() // Network Tab JSONArray
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_tab_row, parent, false)

        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        // Local
        val tab = data[position]
        val tabId = tab.id
        val tabName = tab.name

        /* Bind views */
        val tabNameView = holder.view.findViewById<TextView>(R.id.tabName).apply { text = tab.name }
        val tabTypeIcon = holder.view.findViewById<ImageView>(R.id.tabType)
        val tvTabCurrency = holder.view.findViewById<TextView>(R.id.tvTabCurrency).apply {
            text = Currency.getInstance(tab.currency).getSymbol(Locale.UK)
//            CurrencyHelpers.symbolMap.get(tab.currency)
        }

        val tabBalanceView = holder.view.findViewById<TextView>(R.id.tabBalance)
            .apply {
            text =
                if (tab.balance < 0.0) { (tab.balance * -1.0).round(2).commatize()
                    .also {
                        this.setTextColor(ContextCompat.getColor(holder.view.context, R.color.Watermelon))
                    }
                }
                else if (tab.balance > 0.0) tab.balance.round(2).commatize()
                    .also {
                        this.setTextColor(ContextCompat.getColor(holder.view.context, R.color.AppTheme))
                    }
                else "0.00"
        }

        holder.view.findViewById<ImageView>(R.id.shareTab).setOnClickListener {
            // Hide action menu after it's clicked
            val actionMenu = it.parent as LinearLayoutCompat
            actionMenu.visibility = View.GONE

            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"

                val userName = PreferenceManager.getDefaultSharedPreferences(it.context.applicationContext).getString(
                    PreferenceKey.USER_NAME, "NO NAME")

                val balanceSummary = if (tab.balance > 0f) {
                    "${tab.name} owes $userName"
                } else if (tab.balance < 0.0) {
                    "$userName owes ${tab.name}"
                } else {
                    it.context.resources.getString(R.string.flat_balance_primary)
                }

                var shareText = "$balanceSummary ${tab.currency}\n\nRecent Transactions"

                CoroutineScope(IO).launch {
                    val transactions = db.transactionDao().getTransactionsByTabId(tab.id!!)
                    transactions.forEach { shareText += (it.toString() + "<br>") }
                }

                val shareSpannable = HtmlCompat.fromHtml(shareText, 0)

                putExtra(Intent.EXTRA_TEXT, shareSpannable.toString())
            }

            startActivity(ContextThemeWrapper(it.context, R.style.AppTheme), Intent.createChooser(i, "Share with"), null)
        }

        holder.view.findViewById<ImageView>(R.id.deleteTab).setOnClickListener {
            val actionMenu = it.parent as LinearLayoutCompat
            actionMenu.visibility = View.GONE

            deleteTabDialog(ContextThemeWrapper(it.context, R.style.AlertDialog), tab)
        }

        /**
         * Click listener
         */
        holder.view.setOnClickListener {
            val intent = Intent(
                holder.view.context,
                TabActivity::class.java
            )
            intent.putExtra("TAB_PARCELABLE", TabParcelable(tab.id!!, tab.name, tab.currency))
            startActivity(holder.view.context, intent, null)
        }

        // Long click listener
        holder.view.setOnLongClickListener {
//            val dialog = AlertDialog.Builder(it.context)
//            dialog.setTitle("Delete \"$tabName\"?")
//            dialog.setPositiveButton("OK") { dialog, which ->
//
//                // Local
//                CoroutineScope(IO).launch {
//                    db.tabDao().deleteTabById(tabId!!)
//
//                    CoroutineScope(Main).launch {
//                        val intent = Intent(it.context, MainActivity::class.java).apply {
//                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // finish all activities on top of main activity (and below current activity)
////                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // opens existing activity instead of recreating
//                        }
//
//                        startActivity(it.context, intent,null)
//                        Toast.makeText(it.context, "$tabName deleted", Toast.LENGTH_LONG).show()
////                        val activity = it.context as Activity
////                        activity.finish()
//                    }
//                }
//            }
//            dialog.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
//            dialog.show()
            val menuBar = it.findViewById<LinearLayoutCompat>(R.id.tabOptions)

            if (menuBar.visibility == View.VISIBLE) {
                menuBar.visibility = View.GONE
            } else {
                menuBar.visibility = View.VISIBLE
            }

            true
        }

        /**
         * Tab Balance
         */
//        CoroutineScope(IO).launch {
//            var tabBal = 0.0
//            val ledger = Ledger.getLedger(tab.id!!)
//
//            // Calculate tab balance
//            ledger.forEach { memberBalance ->
//                memberBalance.value.let {
//                    if (it > 0.0) tabBal += it
//                }
//            }
//
//            // Set tab balance view
//            withContext(Main) {
//                tabBalanceView.text = tabBal.round(2).toString()
//            }
//        }
    }

    private fun deleteTabDialog(context: Context, tab: Tab) {
        val builder = MaterialAlertDialogBuilder(context)

        builder.setTitle("${tab.name}")
        builder.setMessage("Are you sure you want to delete \"${tab.name}\"?")
        builder.setPositiveButton("OK") { dialog, which ->
            CoroutineScope(IO).launch {
                db.tabDao().deleteTabById(tab.id!!)

                CoroutineScope(Main).launch {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // finish all activities on top of main activity (and below current activity)
//                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // opens existing activity instead of recreating
                    }

                    startActivity(context, intent,null)
                    Toast.makeText(context, "${tab.name} deleted", Toast.LENGTH_LONG).show()
                }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

        val dialog = builder.show()

        val message = dialog.window?.findViewById<TextView>(android.R.id.message)
        message?.typeface = ResourcesCompat.getFont(context, R.font.quicksand)
        message?.textSize = 16f
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