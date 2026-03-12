package com.savestatus.pro.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class StatusPagerAdapter(
    activity: FragmentActivity,
    private val tabCount: Int = 3,
    private val startTab: Int = 0
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = tabCount

    override fun createFragment(position: Int): Fragment {
        val tabType = when {
            tabCount == 1 -> startTab  // e.g. TAB_DOWNLOADED
            else -> position           // 0=Recent, 1=Images, 2=Videos
        }
        return StatusListFragment.newInstance(tabType)
    }
}
