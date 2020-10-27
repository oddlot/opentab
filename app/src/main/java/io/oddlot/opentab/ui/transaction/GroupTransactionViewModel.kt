package io.oddlot.opentab.ui.transaction

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.oddlot.opentab.db
import io.oddlot.opentab.Allocation
import io.oddlot.opentab.deserialize
import io.oddlot.opentab.utils.round
import io.oddlot.opentab.data.GroupExpense
import io.oddlot.opentab.data.Member
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.math.ceil

private const val TAG = "GROUP EXPENSE VIEW MODEL"

class GroupExpenseViewModel(private val groupExpense: GroupExpense? = null, val memberIds: List<Int>?): ViewModel() {

    val description: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
            it.value = groupExpense?.description ?: ""
        }
    }

    var date: Long = groupExpense?.date ?: 0

    val allocation: MutableLiveData<Allocation> by lazy {
        MutableLiveData<Allocation>().also {
            it.value = groupExpense?.allocation?.deserialize() ?: Allocation()
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

    val payees: MutableLiveData<List<Member>> by lazy {

        MutableLiveData<List<Member>>().also {
            it.value = listOf()
        }

        MutableLiveData<List<Member>>().also {
            var _payees: List<Member> = listOf<Member>() // []

            CoroutineScope(IO).launch {
                _payees = allocation.value!!.keys.map {
                    db.memberDao().getMemberById(it)
                }

                withContext(Main) {
                    it.value = _payees
                }
            }
        }
    }

    val payeeIds: MutableLiveData<List<Int>> by lazy {
        MutableLiveData<List<Int>>().also {
            val alloc: Allocation? = groupExpense?.allocation.deserialize()
            it.value = alloc?.keys?.toList() ?: listOf()
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

    fun equalAllocation(): Allocation {
        val _payeeIds = this.payeeIds.value!!
        val _allocation = Allocation()

        var bpsUnallocated = ceil(amountPaid.value!! * 100).toInt()
        val bpsSplit = ceil((bpsUnallocated.toDouble() / _payeeIds.size)).toInt()

        for (id in _payeeIds) {
            _allocation[id] = bpsSplit.toDouble() / 100
        }

        val bpsAllocated = bpsSplit * _payeeIds.size
        bpsUnallocated -= bpsAllocated

        while (bpsUnallocated < 0) {
            val pos = bpsUnallocated * -1 // proxy for position index as unallocated bps will always be <= number of members
            val memberId = _payeeIds[pos]
            _allocation[memberId] = (bpsSplit / 100.0 - 0.010).round(2)
            bpsUnallocated += 1
        }

        this.allocation.value = _allocation
        return _allocation
    }

    fun update(groupExpense: GroupExpense) {
        db.groupExpenseDao().updateGroupItem(groupExpense)
    }
}

class GroupExpenseViewModelFactory(var groupExpense: GroupExpense? = null, val memberIds: List<Int>? = null) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(GroupExpenseViewModel::class.java)) {
            GroupExpenseViewModel(groupExpense, memberIds) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}