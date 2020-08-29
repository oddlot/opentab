package io.oddlot.ledger.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
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
            tab.text = arrayOf("Tabs", "Transactions", "Placeholder")[position]

        }.attach()
    }
}