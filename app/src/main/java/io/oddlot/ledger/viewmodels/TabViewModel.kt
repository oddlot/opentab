package io.oddlot.ledger.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.oddlot.ledger.db
import io.oddlot.ledger.data.Transaction

class TabViewModel : ViewModel() {
    private var ldItems: LiveData<List<Transaction>>? = null

    // Create a LiveData with list of tabs
//    tabs: LiveData<List<Tab>> by lazy {
//        LiveData<List<Tab>>()
//    }

//    fun getTabBalance(): LiveData<Double> {
//        db.tabDao().get()
//    }

    fun getItems(): LiveData<List<Transaction>> {
        return db.transactionDao().getAllAsLiveData()
    }
}