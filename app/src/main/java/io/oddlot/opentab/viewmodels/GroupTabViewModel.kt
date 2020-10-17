package io.oddlot.opentab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.oddlot.opentab.db
import io.oddlot.opentab.data.GroupExpense
import io.oddlot.opentab.data.Membership
import io.oddlot.opentab.data.Tab

class GroupTabViewModel(tabId: Int) : ViewModel() {

    val liveTab: LiveData<Tab> by lazy {
        db.tabDao().getLiveTabById(tabId)
    }

//    val members: LiveData<List<Member>> by lazy {
//        db.memberDao().getLiveMembersByTabId(tabId)
//    }

    val memberships: LiveData<List<Membership>> by lazy {
        db.membershipDao().getLiveMembershipsByTabId(tabId)
    }

    val items: LiveData<List<GroupExpense>> by lazy {
        db.groupExpenseDao().getLiveGroupItemsByTabId(tabId)
    }

    val balance: MutableLiveData<Double> by lazy {
        MutableLiveData<Double>().also {
            it.value = 0.0
        }
    }

    val currency: MutableLiveData<String> by lazy {
        MutableLiveData<String>().also {
//            it.value = db.tabDao().get(tabId).currency
        }
    }
}

class GroupTabViewModelFactory(var tabId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(GroupTabViewModel::class.java)) {
            GroupTabViewModel(tabId) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}