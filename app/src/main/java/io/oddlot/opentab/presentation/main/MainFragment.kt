package io.oddlot.opentab.presentation.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.oddlot.opentab.R
import io.oddlot.opentab.db
import io.oddlot.opentab.presentation.tab.TabsFragment
import io.oddlot.opentab.presentation.transaction.TransactionsFragment
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MainFragment"

class MainFragment : Fragment() {
    private var mainActivity: AppCompatActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        mainActivity = activity as AppCompatActivity

        val pager = view.findViewById<ViewPager2>(R.id.pager)
        val fragments = listOf(TabsFragment(), TransactionsFragment())

        pager.adapter = MainViewPagerAdapter(mainActivity!!, fragments)

        CoroutineScope(Dispatchers.IO).launch {
            val tabs = db.tabDao().getAll()

            if (tabs.isNotEmpty()) {
                view.findViewById<TextView>(R.id.noTabsPrompt).visibility = View.GONE
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        /**
         * Link tab layout and viewpager
         * and configure tab strategy
         */
        TabLayoutMediator(tabLayout, pager) { tab, position ->
            configureTab(tab, position)
        }.attach()
    }

    override fun onResume() {
        super.onResume()

        this.pager.currentItem
    }

    private fun configureTab(tab: TabLayout.Tab, position: Int) {
        tab.text = arrayOf("Tabs", "Transactions", "Placeholder")[position]

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            val fontId = R.font.quicksand

            override fun onTabSelected(tab: TabLayout.Tab?) {
                Log.d(TAG, "Tab $position Selected")
                val tabText = tab?.view?.getChildAt(1) as TextView

//                tabText.setTypeface(ResourcesCompat.getFont(tabText.context, fontId), Typeface.BOLD)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val tabText = tab?.view?.getChildAt(1) as TextView

//                tabText.setTypeface(ResourcesCompat.getFont(tabText.context, fontId), Typeface.NORMAL)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                val tabText = tab?.view?.getChildAt(1) as TextView

//                tabText.setTypeface(ResourcesCompat.getFont(tabText.context, fontId), Typeface.BOLD)
            }
        })
    }
}