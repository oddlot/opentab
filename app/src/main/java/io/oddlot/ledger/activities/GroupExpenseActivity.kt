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
    private var mGroupExpense: GroupExpense? = null
    private lateinit var mGroupExpenseVM: GroupExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_expense)

        CoroutineScope(IO).launch {
            mGroupExpense = db.groupExpenseDao()
        }

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = resources.getString(R.string.actionbar_title_edit_group_expense)
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }


        if ( == null) {
            groupExpense = GroupExpense()
        } else {

        }

    }
}