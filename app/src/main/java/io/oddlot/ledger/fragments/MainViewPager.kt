package io.oddlot.ledger.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.MainViewPagerAdapter
import kotlinx.android.synthetic.main.fragment_main_viewpager.*


class MainViewPager : Fragment() {
    val TAG = "MAIN_VIEWPAGER_FRAGMENT"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_viewpager, container, false)
        val pager = view.findViewById<ViewPager2>(R.id.pager)
        val fragments = listOf(TabsFragment(), TransactionsFragment())

        pager.adapter = MainViewPagerAdapter(activity as AppCompatActivity, fragments)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        TabLayoutMediator(tabLayout, pager) { tab, position ->
//            tab.text = arrayOf("Tabs", "Transactions", "Placeholder")[position]

            if (position == pager.currentItem) {
//                val ttv = tab.view.getChildAt(1) as TextView
//                ttv.setTypeface(ResourcesCompat.getFont(ttv.context, R.font.rajdhani), Typeface.BOLD)
            }

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    Log.d(TAG, "Tab $position Selected")
                    val tabText = tab?.view?.getChildAt(1) as TextView
                    tabText.setTypeface(ResourcesCompat.getFont(tabText.context, R.font.rajdhani), Typeface.BOLD)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    Log.d(TAG, "Tab $position Unselected")
                    val tabText = tab?.view?.getChildAt(1) as TextView
                    tabText.setTypeface(ResourcesCompat.getFont(tabText.context, R.font.rajdhani), Typeface.NORMAL)
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    Log.d(TAG, "Tab $position Reselected")
                    val tabText = tab?.view?.getChildAt(1) as TextView
                    tabText.setTypeface(ResourcesCompat.getFont(tabText.context, R.font.rajdhani), Typeface.BOLD)
                }
            })
        }.attach() // Link tab layout and viewpager together
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "resuming")

        this.pager.currentItem
    }
}