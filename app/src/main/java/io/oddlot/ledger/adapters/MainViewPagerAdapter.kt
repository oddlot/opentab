package io.oddlot.ledger.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainViewPagerAdapter(activity: AppCompatActivity, private val fragments: List<Fragment>) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

}