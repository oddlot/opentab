package io.oddlot.opentab.ui.tab

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import io.oddlot.opentab.R
import io.oddlot.opentab.data.*
import io.oddlot.opentab.db
import io.oddlot.opentab.parcelables.TabParcelable
import kotlinx.android.synthetic.main.activity_tab.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

//class TabActivity : AppCompatActivity() {
class TabFragment : Fragment() {

    private val TAG = this::class.java.simpleName
    private lateinit var viewModel: TabViewModel

    private var mTabBalance = 0.0
    private var transactions: MutableList<Transaction> = mutableListOf()
    private lateinit var tabParcelable: TabParcelable
    private lateinit var tab: Tab

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_tab, container, false)
//        return super.onCreateView(inflater, container, savedInstanceState)

//        newTransactionFab.setOnClickListener { fabListener("click") }
//        newTransactionFab.setOnLongClickListener { fabListener("longClick") }

        return view
    }
}