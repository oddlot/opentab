package io.oddlot.ledger.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.oddlot.ledger.db
import io.oddlot.ledger.data.Tab


class TabsViewModel : ViewModel() {
//    private var tabs: LiveData<List<Tab>>? = null

    // Create a LiveData with list of tabs
    private val tabs: LiveData<List<Tab>> by lazy {
        MutableLiveData<List<Tab>>().also {
            db.tabDao().allTabsAsLiveData()
        }
    }

    fun getAll(): LiveData<List<Tab>> {
        return db.tabDao().allTabsAsLiveData()
    }
}