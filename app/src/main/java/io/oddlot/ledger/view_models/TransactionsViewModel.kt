package io.oddlot.ledger.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.oddlot.ledger.activities.db
import io.oddlot.ledger.data.Transaction


class TransactionsViewModel : ViewModel() {
    private var items: LiveData<List<Transaction>>? = null

    // Create a LiveData with list of tabs
//    tabs: LiveData<List<Tab>> by lazy {
//        LiveData<List<Tab>>()
//    }

//    fun getTabBalance(): LiveData<Double> {
//        db.tabDao().get()
//    }

    fun getItems(): LiveData<List<Transaction>> {
        return db.itemDao().getAllAsLiveData()
    }
}