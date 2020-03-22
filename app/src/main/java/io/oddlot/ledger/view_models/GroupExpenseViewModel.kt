package io.oddlot.ledger.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.Allocation
import io.oddlot.ledger.deserialize
import io.oddlot.ledger.utils.round
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.data.Member
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.math.ceil

private const val TAG = "GROUP EXPENSE VIEW MODEL"

class GroupExpenseViewModel(private val groupExpense: GroupExpense? = null): ViewModel() {

    init {
        groupExpense?.let {
            fromGroupExpense(it)
        }
    }

    private fun fromGroupExpense(groupExpense: GroupExpense) {
        this.date = groupExpense.date
        this.amountPaid.value = groupExpense.amount
        this.payer.value = db.memberDao().getMemberById(this.payerId)
        this.description.value = groupExpense.description ?: ""
        this.allocation.value = groupExpense.allocation?.deserialize() ?: Allocation()
    }

    val description: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            it.value = ""
        }
    }

    var date: Long = groupExpense?.date ?: 0

    val allocation: MutableLiveData<Allocation> by lazy {
        MutableLiveData<Allocation>().also {
            it.value = Allocation()
        }
    }

    val payer: MutableLiveData<Member> = MutableLiveData<Member>().also {
        var payer: Member
        CoroutineScope(IO).launch {
            payer = db.memberDao().getMemberById(0)

            withContext(Main) {
                it.value = payer
            }
        }
    }

    var payerId: Int = 0

    val amountPaid: MutableLiveData<Double> by lazy {
        MutableLiveData<Double>().also {
            it.value = groupExpense?.amount ?: 0.0
        }
    }

    val livePayees: MutableLiveData<List<Member>> by lazy {
        MutableLiveData<List<Member>>().also {
            var _payees: List<Member> = listOf<Member>() // []

            CoroutineScope(Dispatchers.IO).launch {
                _payees = allocation.value!!.keys.map {
                    db.memberDao().getMemberById(it)
                }

                withContext(Dispatchers.Main) {
                    it.value = _payees
                }
            }
        }
    }

    var payees: List<Member> = listOf()

    fun allocated(): Double {
        var amount = 0.0

        this.allocation.value?.forEach {
            amount += it.value!!
        }

        return amount
    }

    fun unallocated(): Double = amountPaid.value!!.minus(allocated()).round(2)

    fun equalAllocation(payees: List<Member> = this.livePayees.value!!): Allocation {
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

        this.allocation.value = allocation
        return allocation
    }

    fun update(groupExpense: GroupExpense) {
        db.groupExpenseDao().updateGroupItem(groupExpense)
    }
}

class GroupExpenseViewModelFactory(var groupExpense: GroupExpense? = null) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(GroupExpenseViewModel::class.java)) {
            GroupExpenseViewModel(groupExpense) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}