package io.oddlot.ledger.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.classes.Allocation
import io.oddlot.ledger.classes.deserialize
import io.oddlot.ledger.classes.round
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.data.Member
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread
import kotlin.math.ceil

class GroupExpenseViewModel(val tabId: Int, val groupExpense: GroupExpense? = null): ViewModel() {

    val description: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            it.value = groupExpense?.description ?: ""
        }
    }

    val date: MutableLiveData<Long> by lazy {
        MutableLiveData<Long>()
    }

    val allocation: MutableLiveData<Allocation> by lazy {
        MutableLiveData<Allocation>().also {
            it.value = groupExpense?.allocation?.deserialize() ?: Allocation()
        }
    }

    val payer: MutableLiveData<Member> by lazy {
        MutableLiveData<Member>()
    }

    val amountPaid: MutableLiveData<Double> by lazy {
        MutableLiveData<Double>().also {
            it.value = groupExpense?.amount ?: 0.0
        }
    }

    val payees: MutableLiveData<List<Member>> by lazy {
        MutableLiveData<List<Member>>().also {
            var _payees: List<Member> = listOf<Member>() // []

            CoroutineScope(Dispatchers.IO).launch {
                var alloc = groupExpense?.allocation?.deserialize()
                _payees = alloc?.keys!!.map {
                    db.memberDao().getMemberById(it)
                }

                withContext(Dispatchers.Main) {
                    it.value = _payees
                }
            }
        }
    }

    fun allocated(): Double {
        var amount = 0.0

        this.allocation.value?.forEach {
            amount += it.value!!
        }

        return amount
    }

    fun unallocated(): Double = amountPaid.value!!.minus(allocated()).round(2)

    fun equalAllocation(payees: List<Member>): Allocation {
        val allocation = Allocation()
        allocation.payees = payees.toMutableSet()

        var bpsUnallocated = ceil(amountPaid.value!! * 100).toInt()
        val bpsSplit = ceil((bpsUnallocated.toDouble() / payees.size)).toInt()

        for (m in payees) {
            allocation[m.id!!] = bpsSplit.toDouble() / 100
        }

        val bpsAllocated = bpsSplit * payees.size
        bpsUnallocated -= bpsAllocated

        while (bpsUnallocated < 0) {
            val pos = bpsUnallocated * -1 // proxy for position index as unallocated bps will always be <= number of members
            val member = payees[pos]
            allocation[member.id!!] = (bpsSplit / 100.0 - 0.010).round(2)
            bpsUnallocated += 1
        }

        return allocation
    }

    // Create and insert new Group Item
    fun submit(): GroupExpense {
        val groupItem = GroupExpense(
            null,
            tabId = tabId,
            payerId = payer.value!!.id!!,
            amount = amountPaid.value!!,
            description = description.value,
            date = date.value!!,
            allocation = allocation.value!!.serialize()
        ).also {
            thread {
                db.groupExpenseDao().insert(it)
            }
        }

        return groupItem
    }

    fun stage(groupItemId: Int? = null): GroupExpense {
        return GroupExpense(
            id = groupItemId,
            tabId = tabId,
            payerId = payer.value!!.id!!,
            amount = amountPaid.value!!,
            description = description.value,
            date = date.value!!,
            allocation = allocation.value!!.serialize()
        )
    }

    fun update(groupExpense: GroupExpense) {
        db.groupExpenseDao().updateGroupItem(groupExpense)
    }
}

class GroupItemViewModelFactory(var tabId: Int, var groupExpense: GroupExpense? = null) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(GroupExpenseViewModel::class.java)) {
            GroupExpenseViewModel(tabId, groupExpense) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}