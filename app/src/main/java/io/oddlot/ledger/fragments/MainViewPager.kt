package io.oddlot.ledger.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.oddlot.ledger.R
import io.oddlot.ledger.adapters.MainViewPagerAdapter
import io.oddlot.ledger.view_models.TabsViewModel
import kotlinx.android.synthetic.main.activity_main_inprogress.*
import kotlinx.android.synthetic.main.activity_main_inprogress.view.*
import kotlinx.android.synthetic.main.fragment_main_viewpager.*


class MainViewPagerFragment : Fragment() {
    val TAG = "MAIN_VIEWPAGER_FRAGMENT"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_viewpager, container, false)

        val fragments = listOf(TabsFragment(), TransactionsFragment())



        val vp = view.findViewById<ViewPager2>(R.id.viewPager).apply {
            adapter = MainViewPagerAdapter(activity as AppCompatActivity, fragments)
        }

        val tabLayoutMediator = TabLayoutMediator(fragments, vp)

        return view
    }
}