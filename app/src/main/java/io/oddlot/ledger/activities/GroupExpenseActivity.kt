package io.oddlot.ledger.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.oddlot.ledger.R
import io.oddlot.ledger.data.GroupExpense
import io.oddlot.ledger.view_models.GroupExpenseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class GroupExpenseActivity : AppCompatActivity() {
    private lateinit var mGroupExpense: GroupExpense
    private lateinit var mGroupExpenseVM: GroupExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_expense)

        if (intent.extras?.get("GROUP_ITEM_ID") == null) {
            groupExpense = GroupExpense()
        } else {

        }

    }
}