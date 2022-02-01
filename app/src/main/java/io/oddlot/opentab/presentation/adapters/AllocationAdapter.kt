package io.oddlot.opentab.presentation.adapters

import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.oddlot.opentab.R
import io.oddlot.opentab.Allocation
import io.oddlot.opentab.data.Member
import io.oddlot.opentab.presentation.transaction.GroupExpenseViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

//class AllocationAdapter(val allocation: Allocation) : RecyclerView.Adapter<AllocationAdapter.PayeeViewHolder>() {
class AllocationAdapter(var vmGroupExpense: GroupExpenseViewModel, val members: List<Member>, var allocation: Allocation) : RecyclerView.Adapter<AllocationAdapter.PayeeViewHolder>() {
    val TAG = "ALLOCATION_ADAPTER"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PayeeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.payee_allocation_row, parent, false)
        val holder = PayeeViewHolder(view)

        return holder
    }

    override fun getItemCount(): Int {
        return members.size
    }

    override fun onBindViewHolder(holder: PayeeViewHolder, position: Int) {
        val nameView = holder.view.findViewById<TextView>(R.id.payeeName)
        val amountView = holder.view.findViewById<EditText>(R.id.allocatedAmount)
        val payee = members[position]

        // Color IDs
        val white = ContextCompat.getColor(holder.view.context, R.color.DefaultWhite)
        val appColor = ContextCompat.getColor(holder.view.context, R.color.AppTheme)
        val primaryColor = ContextCompat.getColor(holder.view.context, R.color.BlueBlack)
        val secondaryColor = ContextCompat.getColor(holder.view.context, R.color.BlueBlack)

        // Name views
        nameView.text = payee.name.also {
            allocation[payee.id!!]?.let {
                if (it > 0.0) {
                    nameView.setBackgroundColor(appColor)
                    nameView.setTextColor(white)
                }
            }
        }

        // Click listener
        nameView.setOnClickListener {
            var _payees = vmGroupExpense.payees.value ?: listOf()

            if (payee in _payees) {
                vmGroupExpense.payees.value = _payees.minus(payee)
                nameView.setBackgroundColor(white)
                nameView.setTextColor(secondaryColor)

            } else {
                vmGroupExpense.payees.value = _payees.plus(payee)
                nameView.setBackgroundColor(appColor)
                nameView.setTextColor(white)
            }
        }

        // Bind allocated amount to view
        allocation[payee.id]?.let {
            amountView.text = SpannableStringBuilder(it.toString())
        }
//        groupExpense.allocation.value!![payee.id]?.let {
//            amountView.text = SpannableStringBuilder(it.toString())
//        }

        CoroutineScope(IO).launch {
            // Amount allocated
            amountView.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, count: Int) {
                        when (p0.toString()) {
                            "" -> {
                                allocation[payee.id!!] = null
                            }
                            "." -> false
                            else -> {
                                Log.d(TAG, payee.name + payee.id.toString())
                                if (count > 0) {
                                    Log.d(TAG, "$count")
                                    allocation[payee.id!!] = text.toString().toDouble()
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    class PayeeViewHolder(val view: View): RecyclerView.ViewHolder(view)
}