package io.oddlot.ledger

import android.content.Context
import android.widget.TextView
import io.oddlot.ledger.db
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.data.Member
import io.oddlot.ledger.utils.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

class Ledger(tabId: Int?): HashMap<String, Double>() {
    var mMembers: List<Member>? = null

    init {
        if (tabId != null) {
            CoroutineScope(IO).launch {
                mMembers = db.memberDao().getMembersByTabId(tabId)

                mMembers?.forEach { this@Ledger[it.name] = 0.0 }
            }
        }
    }

    companion object {
        suspend fun getLedger(tabId: Int): Ledger {
            val ledger = Ledger(tabId) // { Dan=0.0, Thomas=0.0, Brian=0.0, Sherry=0.0, Cecilia=null }
            var members: List<Member>? = null
            var groupExpenses: List<GroupExpense>? = null

            withContext(IO) {
                members = db.memberDao().getMembersByTabId(tabId)
                groupExpenses = db.groupExpenseDao().getGroupExpenseByTabId(tabId)
            }

            groupExpenses?.forEach { groupExpense ->
                // Get item allocation
                val dsrMap = groupExpense.allocation!!.deserialize() // { 1=28.0, 3=28.0 }

                withContext(IO) {
                    // Update paid amounts
                    val payer = db.memberDao().getMemberById(groupExpense.payerId)
                    if (payer.name in ledger.keys) {
                        ledger[payer.name] = ledger[payer.name]!!.minus(groupExpense.amount)
                    }

                    // Update owed amounts
                    members?.forEach { m ->
                        dsrMap[m.id]?.let { allocatedAmount ->
//                            ledger[m.name] = ledger[m.name]!!.plus(allocatedAmount)
                            ledger[m.name]?.let {
                                ledger[m.name] = it + allocatedAmount
                            }
                        }
                    }
                }
            }

            return ledger
        }
    }

    fun asTextView(context: Context): TextView {
        val view = TextView(context).apply { setPadding(80, 20, 0, 0) }
        var textString = ""

        this.forEach {
            val isOwedOrNot = it.value.let { amount ->
                if (amount < 0.0) "is owed" else "owes"
            }

            textString += "${it.key} $isOwedOrNot ${it.value.absoluteValue.round(2)}\n"
        }

        view.text = textString

        return view
    }
}