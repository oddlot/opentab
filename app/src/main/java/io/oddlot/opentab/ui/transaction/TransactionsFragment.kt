package io.oddlot.opentab.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.oddlot.opentab.R
import io.oddlot.opentab.db
import io.oddlot.opentab.ui.adapters.TransactionAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class TransactionsFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transactions, container, false)
        val rv = view.findViewById<RecyclerView>(R.id.transactionsRecyclerView)

        rv.apply {
            layoutManager = LinearLayoutManager(activity)

            CoroutineScope(IO).launch {
                val transactions = db.transactionDao().getAll()
                CoroutineScope(Main).launch {
                    adapter = TransactionAdapter(transactions.sortedDescending())
                }
            }
        }

        return view
    }
}