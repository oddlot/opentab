package io.oddlot.opentab.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.oddlot.opentab.R
import io.oddlot.opentab.adapters.MainViewPagerAdapter
import io.oddlot.opentab.db
import kotlinx.android.synthetic.main.fragment_main_viewpager.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainFragment : Fragment() {
    val TAG = this::class.java.simpleName
    var mainActivity: AppCompatActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_viewpager, container, false)

        mainActivity = activity as AppCompatActivity

        val pager = view.findViewById<ViewPager2>(R.id.pager)
        val fragments = listOf(TabsFragment(), TransactionsFragment())


        pager.adapter = MainViewPagerAdapter(mainActivity!!, fragments)

        CoroutineScope(Dispatchers.IO).launch {
            val tabs = db.tabDao().allTabs()

            if (tabs.isNotEmpty()) {
                view.findViewById<TextView>(R.id.noTabsPrompt).visibility = View.GONE
            }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "starting")

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
        Log.d(TAG, "resuming")

        this.pager.currentItem
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "destroying")
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun configureTab(tab: TabLayout.Tab, position: Int) {
        tab.text = arrayOf("Tabs", "Transactions", "Placeholder")[position]

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            val fontId = R.font.quicksand

            override fun onTabSelected(tab: TabLayout.Tab?) {
                Log.d(TAG, "Tab $position Selected")
                val tabText = tab?.view?.getChildAt(1) as TextView
                tabText.setTypeface(ResourcesCompat.getFont(tabText.context, fontId), Typeface.BOLD)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                Log.d(TAG, "Tab $position Unselected")
                val tabText = tab?.view?.getChildAt(1) as TextView
                tabText.setTypeface(ResourcesCompat.getFont(tabText.context, fontId), Typeface.NORMAL)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d(TAG, "Tab $position Reselected")
                val tabText = tab?.view?.getChildAt(1) as TextView
                tabText.setTypeface(ResourcesCompat.getFont(tabText.context, fontId), Typeface.BOLD)
            }
        })
    }
}