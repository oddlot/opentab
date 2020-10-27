package io.oddlot.opentab.ui.transaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.oddlot.opentab.parcelables.TransactionParcelable
import io.oddlot.opentab.data.Member
import java.util.*

class TransactionViewModel : ViewModel() {
    var date = Date()
    var tabId = 0
    var amount = 0.0
    var description: String? = null
    var isTransfer = false
    val selectedMembers: MutableLiveData<List<String>> by lazy {
        MutableLiveData<List<String>>()
    }

    fun init(txn: TransactionParcelable) {
        this.date = Date(txn.date)
        this.tabId = txn.tabId
        this.amount = txn.amount
        this.description = txn.description

        if (txn.isTransfer == 1) this.isTransfer = true
    }

    override fun toString(): String{
        return "\nDate: $date, \nTab ID: $tabId, \nAmount: $amount, \nDescription: $description, \nIs Transfer: $isTransfer"
    }
}

//class TransactionViewModelFactory(var txnId: Int) : ViewModelProvider.Factory {
//    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//        return if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
//            TransactionViewModel(txnId) as T
//        } else {
//            throw IllegalArgumentException("ViewModel Not Found")
//        }
//    }
//}