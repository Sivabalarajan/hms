package com.android.hms.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.android.hms.R
import com.android.hms.databinding.ActivityOwnerMainBinding
import com.android.hms.model.Users
import com.android.hms.ui.fragments.*
import com.android.hms.ui.reports.ExpensesAndRepairsSummaryReportActivity
import com.android.hms.ui.reports.ExpensesSummaryReportActivity
import com.android.hms.ui.reports.RepairsSummaryReportActivity
import com.android.hms.ui.reports.RentsSummaryReportActivity
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class OwnerMainActivity : BaseActivity() {

//    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityOwnerMainBinding

//    private lateinit var dashboardFragment: DashboardFragment
//    private lateinit var buildingsFragment: BuildingsFragment
//    private lateinit var housesFragment: HousesFragment
//    private lateinit var buildingsHousesFragment: BuildingsHousesFragment
//    private lateinit var notClosedRepairsFragment: NotClosedRepairsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val progressBar = MyProgressBar(this)
        binding = ActivityOwnerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarOwnerMain.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // binding.appBarMain.toolbar.setNavigationIcon(R.mipmap.ic_launcher_foreground)

        initializeDrawerMenus()

        initializeBottomTabs()

        initializeObservers()

        if (savedInstanceState == null) loadFragment(DashboardFragment())

        progressBar.dismiss()
    }

    private fun initializeDrawerMenus() {
        initDrawerHeaders(binding.drawerNavView.getHeaderView(0))
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val drawerNavView: NavigationView = binding.drawerNavView
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
//        appBarConfiguration = AppBarConfiguration(setOf(R.id.navigation_dashboard), drawerLayout)
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        drawerNavView.setupWithNavController(navController)
        drawerNavView.setNavigationItemSelectedListener { menuItem -> onClickDrawerMenuItem(menuItem) }

        // showing Hamburger icon
        val toggle = ActionBarDrawerToggle(this, drawerLayout, binding.appBarOwnerMain.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        if (!(Users.isCurrentUserOwner() || Users.isCurrentUserAdmin())) {
            drawerNavView.menu.findItem(R.id.nav_rental_summary_report)?.isVisible = false
            drawerNavView.menu.findItem(R.id.nav_repairs_summary_report)?.isVisible = false
            drawerNavView.menu.findItem(R.id.nav_view_all_rents)?.isVisible = false
            drawerNavView.menu.findItem(R.id.nav_view_all_repairs)?.isVisible = false
            drawerNavView.menu.findItem(R.id.nav_users_management)?.isVisible = false
        }
    }

    private fun initializeBottomTabs() {
        val bottomNavView:BottomNavigationView = findViewById(R.id.bottom_nav_view)
//        val bottomNavController = findNavController(R.id.nav_host_fragment_bottom_tab)
//        bottomNavView.setupWithNavController(bottomNavController)
        bottomNavView.setOnItemSelectedListener { menuItem -> onClickBottomTab(menuItem) }
        // loadFragment(DashboardFragment())
    }

    private fun initializeObservers() {

        if (!SharedViewModelSingleton.refreshListsEvent.hasObservers()) SharedViewModelSingleton.refreshListsEvent.observe(this) {
            // refresh dashboards
        }

        SharedViewModelSingleton.selectedBuildingEvent.observe(this) { building ->
            val bundle = Bundle().apply {
                putString("buildingId", building.id)
                putString("buildingName", building.name)
            }
//            bottomNavController.navigateUp()
            // bottomNavController.navigate(R.id.navigation_houses, bundle)
            loadFragment(HousesFragment(building.id, building.name))
        }
    }

    // Inflate the menu; this adds items to the action bar if it is present.
    /* override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return true
    } */

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                // onBackPressedDispatcher.onBackPressed() // Go back to the previous activity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onClickDrawerMenuItem(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_rental_summary_report -> {
                startActivity(Intent(this, RentsSummaryReportActivity::class.java))
            }
            R.id.nav_repairs_summary_report -> {
                startActivity(Intent(this, RepairsSummaryReportActivity::class.java))
            }
            R.id.nav_expenses_summary_report -> {
                startActivity(Intent(this, ExpensesSummaryReportActivity::class.java))
            }
            R.id.nav_expenses_and_repairs_summary_report-> {
                startActivity(Intent(this, ExpensesAndRepairsSummaryReportActivity::class.java))
            }
            R.id.nav_view_all_rents-> {
                startActivity(Intent(this, RentsReportActivity::class.java))
            }
            R.id.nav_view_all_repairs-> {
                LaunchUtils.showRepairsReportActivity(this, RepairReportType.ALL_REPAIRS)
            }
            R.id.nav_view_all_expenses-> {
                startActivity(Intent(this, ExpensesReportActivity::class.java))
            }
            R.id.nav_users_management -> {
                startActivity(Intent(this, ManageUsersActivity::class.java))
            }
            R.id.nav_logout -> {
                logout()
            }
            else -> {
                // binding.drawerLayout.closeDrawer(binding.drawerNavView)
            }
        }
        binding.drawerLayout.closeDrawer(binding.drawerNavView)
        menuItem.isChecked = false
        return true
    }

    private fun onClickBottomTab(menuItem: MenuItem) : Boolean {
        when (menuItem.itemId) {
            R.id.navigation_dashboard -> {
                // dashboardFragment = DashboardFragment()
                loadFragment(DashboardFragment())
            }

            R.id.navigation_buildings -> {
                loadFragment(BuildingsFragment())
            }

            R.id.navigation_houses -> {
                loadFragment(HousesFragment())
            }

            R.id.navigation_buildings_houses -> {
                loadFragment(BuildingsHousesFragment())
            }

            R.id.navigation_repairs -> {
                loadFragment(NotClosedRepairsFragment())
            }
        }
        return true
    }

    override fun filter(searchText: String) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as BaseFragment?
        // val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull() as BaseFragment?
        currentFragment?.filter(searchText)
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
//            .replace(R.id.nav_host_fragment_bottom_tab, fragment)
            .replace(R.id.fragment_container, fragment)
            .commit()
        return true
    }

    companion object {
        private const val TAG = "OwnerMainActivity"
    }
}