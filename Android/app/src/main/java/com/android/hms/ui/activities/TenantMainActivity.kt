package com.android.hms.ui.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.hms.R
import com.android.hms.databinding.ActivityTenantMainBinding
import com.android.hms.model.Houses
import com.android.hms.model.Users
import com.android.hms.ui.fragments.BaseFragment
import com.android.hms.ui.fragments.TenantHouseFragment
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TenantMainActivity : BaseActivity() {

    private lateinit var binding: ActivityTenantMainBinding

    private var pagerAdapter: ViewPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTenantMainBinding.inflate(layoutInflater)
        setContentView(binding.root)  // setContentView(R.layout.activity_tenant_main)
        setSupportActionBar(binding.appBarTenantMain.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setActionBarView("House Information and Reports")

        val progressBar = MyProgressBar(context)
        initializeDrawerMenus()
        createTabs()
        progressBar.dismiss()
    }

    private fun initializeDrawerMenus() {
        initDrawerHeaders(binding.drawerNavView.getHeaderView(0))
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val drawerNavView: NavigationView = binding.drawerNavView
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.navigation_dashboard), drawerLayout)
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        drawerNavView.setupWithNavController(navController)
        drawerNavView.setNavigationItemSelectedListener { menuItem -> onClickDrawerMenuItem(menuItem) }

        // showing Hamburger icon
        val toggle = ActionBarDrawerToggle(this, drawerLayout, binding.appBarTenantMain.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        drawerNavView.menu.findItem(R.id.nav_rental_summary_report)?.isVisible = false
        drawerNavView.menu.findItem(R.id.nav_repairs_summary_report)?.isVisible = false
        drawerNavView.menu.findItem(R.id.nav_view_all_rents)?.isVisible = false
        drawerNavView.menu.findItem(R.id.nav_view_all_repairs)?.isVisible = false
        drawerNavView.menu.findItem(R.id.nav_users_management)?.isVisible = false
    }

    private fun onClickDrawerMenuItem(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_logout -> { logout() }
            else -> {
                // binding.drawerLayout.closeDrawer(binding.drawerNavView)
            }
        }
        binding.drawerLayout.closeDrawer(binding.drawerNavView)
        menuItem.isChecked = false
        return true
    }

    private fun createTabs() {
        val houses = Houses.getTenantHouses(Users.currentUser?.id ?: return) // id can't be null
        if (houses.isEmpty()) {
            CommonUtils.showMessage(context, "No houses found", "No houses associated with you. Please contact your landlord / house owner.")
            // finish()
            return
        }
        val houseFragments = houses.map { TenantHouseFragment(it) }
        pagerAdapter = ViewPagerAdapter(this, houseFragments)
        binding.appBarTenantMain.viewPager.setUserInputEnabled(false)
        binding.appBarTenantMain.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.appBarTenantMain.tabLayout, binding.appBarTenantMain.viewPager) { tab, position -> tab.text = houseFragments[position].getName() }.attach()

        binding.appBarTenantMain.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                handleTabSelection(tab?.position ?: return)
            } // Get the selected tab position

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.appBarTenantMain.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() { // Add a ViewPager2 page change listener
            override fun onPageSelected(position: Int) {
                handleTabSelection(position)
            } // You can handle page changes here as well
        })
    }

    override fun filter(searchText: String) {
        val position = binding.appBarTenantMain.viewPager.currentItem
        val currentFragment = pagerAdapter?.getFragment(position) as BaseFragment?
        currentFragment?.filter(searchText)
    }

    // Handle tab selection (you can update UI or take actions based on selected tab)
    private fun handleTabSelection(position: Int) {
        invalidateOptionsMenu()  // Invalidate menu to update based on selected tab
        when (position) {
            0 -> {
            }

            1 -> {
            }
        }
    }

    class ViewPagerAdapter(activity: FragmentActivity, private val fragments: List<Fragment>) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
        fun getFragment(position: Int): Fragment? {
            return if (position in 0 until itemCount) fragments[position] else null
        }
    }
}