package io.oddlot.opentab.ui.tab

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.oddlot.opentab.db
import io.oddlot.opentab.data.Transaction

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