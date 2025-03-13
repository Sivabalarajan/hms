package com.android.hms.ui.activities

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.hms.databinding.ActivityBuildingReportsBinding
import com.android.hms.ui.HouseActions
import com.android.hms.ui.fragments.BaseFragment
import com.android.hms.ui.fragments.BuildingRepairReportsFragment
import com.android.hms.ui.fragments.BuildingRentalReportsFragment
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.google.android.material.tabs.TabLayoutMediator
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class BuildingReportsActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityBuildingReportsBinding
    private lateinit var repairReportsFragment: BuildingRepairReportsFragment
    private lateinit var rentReportsFragment: BuildingRentalReportsFragment
    private var pagerAdapter: ViewPagerAdapter? = null

    private var selectedTabPosition = 0  // Variable to track selected tab
    private var buildingId = ""
    private var buildingName = ""

    override fun getReportName(): String { return "Building $buildingName Reports" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBuildingReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)  // setContentView(R.layout.activity_building_reports)
        setActionBarView(getReportName())

        buildingId = intent.getStringExtra("buildingId") ?: ""
        buildingName = intent.getStringExtra("buildingName") ?: ""
        if (buildingId.isEmpty() || buildingName.isEmpty()) {
            CommonUtils.showMessage(context, "Building", "Not able to get the building details. Please try again later.")
            return
        }

        val progressBar = MyProgressBar(context)
        setActionBarView(getReportName())
        initTabs()
        progressBar.dismiss()
    }

    private fun initTabs() {
        repairReportsFragment = BuildingRepairReportsFragment(buildingId, buildingName)
        rentReportsFragment = BuildingRentalReportsFragment(buildingId, buildingName)
        val fragments = listOf(repairReportsFragment, rentReportsFragment)
        pagerAdapter = ViewPagerAdapter(this, fragments)
        binding.viewPager.setUserInputEnabled(false)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position -> tab.text = if (position == 0) "Open Repairs" else "Vacant Rentals" }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                handleTabSelection(tab?.position ?: return)
            } // Get the selected tab position

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() { // Add a ViewPager2 page change listener
            override fun onPageSelected(position: Int) {
                handleTabSelection(position)
            } // You can handle page changes here as well
        })
    }

    // Handle tab selection (you can update UI or take actions based on selected tab)
    private fun handleTabSelection(position: Int) {
        selectedTabPosition = position
        invalidateOptionsMenu()  // Invalidate menu to update based on selected tab
        when (position) {
            0 -> {
                // Repairs tab is selected
                println("Selected Repairs Tab")
            }

            1 -> {
                // Rents tab is selected
                println("Selected Rents Tab")
            }
        }
    }

    override fun filter(searchText: String) {
        val position = binding.viewPager.currentItem
        val currentFragment = pagerAdapter?.getFragment(position) as BaseFragment?
        currentFragment?.filter(searchText)
    }

    class ViewPagerAdapter(activity: FragmentActivity, private val fragments: List<Fragment>) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
        fun getFragment(position: Int): Fragment? { return if (position in 0 until itemCount) fragments[position] else null }
    }

    override fun saveToOutputStream(outputStream: OutputStream) {
        val workbook = XSSFWorkbook()
        createOpenRepairSheet(workbook)
        createVacantRentalsSheet(workbook)
        workbook.write(outputStream)
        workbook.close()
    }

    private fun createOpenRepairSheet(workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet("$buildingName Open Repairs")
        var rowIndex = 0
        val repairsList = repairReportsFragment.getList()
        if (repairsList.isEmpty()) {
            createHeaderExcelCell(sheet.createRow(rowIndex), 0, "No repairs found")
            return sheet
        }

        // Populate data rows
        for ((headingName, houseRepairsList) in repairsList) {
            val headingRow = sheet.createRow(rowIndex++)
            createHeaderExcelCell(headingRow, 0, headingName)
            rowIndex = createRepairSheet(sheet, houseRepairsList, rowIndex)
            sheet.createRow(rowIndex++)
            sheet.createRow(rowIndex++)
        }
        val totalRow = sheet.createRow(rowIndex)
        createHeaderExcelCell(totalRow, 0, "Total")

        // find total from the amount field from the repairsList
        var total = 0.0
        repairsList.forEach { (_, houseRepairsList) ->
            houseRepairsList.forEach { repair ->
                total += repair.amount
            }
        }
        createHeaderExcelCell(totalRow, 4, total)

        autoAdjustRowsColumnsHeight(sheet)

        return sheet
    }

    private fun createVacantRentalsSheet(workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet("$buildingName Vacant Rentals")
        var rowIndex = 0

        var row = sheet.createRow(rowIndex++)
        val depositNotPaidList = rentReportsFragment.depositNotPaidList()
        if (depositNotPaidList.isEmpty()) createHeaderExcelCell(row, 0, "No security deposit not paid available. All security deposits are paid in this building $buildingName.")
        else {
            createHeaderExcelCell(row, 0, "Deposit Not Paid Tenants")
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "Tenant")
            createHeaderExcelCell(row, 1, "House")
            createHeaderExcelCell(row, 2, "Days Pending")
            createHeaderExcelCell(row, 3, "Days Pending Info")
            for (house in depositNotPaidList) {
                row = sheet.createRow(rowIndex++)
                createExcelCell(row, 0, house.tName)
                val nameCell = createExcelCell(row, 1, house.name)
                val depositPendingDays = house.depositPendingDays()
                createExcelCell(row, 2, depositPendingDays)
                createExcelCell(row, 3, HouseActions.depositPendingDaysText(depositPendingDays))
                addCommentToExcelCell(sheet, nameCell, house.notes)
            }
        }
        sheet.createRow(rowIndex++) // empty row
        sheet.createRow(rowIndex++) // empty row

        row = sheet.createRow(rowIndex++)
        val rentNotPaidList = rentReportsFragment.rentNotPaidList()
        if (rentNotPaidList.isEmpty()) createHeaderExcelCell(row, 0, "No rent not paid available. All rents are paid in this building $buildingName.")
        else {
            createHeaderExcelCell(row, 0, "Rent Not Paid Tenants")
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "Tenant")
            createHeaderExcelCell(row, 1, "House")
            createHeaderExcelCell(row, 2, "Days Pending")
            createHeaderExcelCell(row, 3, "Days Pending Info")
            for (house in rentNotPaidList) {
                row = sheet.createRow(rowIndex++)
                createExcelCell(row, 0, house.tName)
                val nameCell = createExcelCell(row, 1, house.name)
                val rentPendingDays = house.rentPendingDays()
                createExcelCell(row, 2, rentPendingDays)
                createExcelCell(row, 3, HouseActions.rentPendingDaysText(rentPendingDays))
                addCommentToExcelCell(sheet, nameCell, house.notes)
            }
        }
        sheet.createRow(rowIndex++) // empty row
        sheet.createRow(rowIndex++) // empty row

        row = sheet.createRow(rowIndex++)
        val vacantHousesList = rentReportsFragment.vacantHousesList()
        if (vacantHousesList.isEmpty()) createHeaderExcelCell(row, 0, "No vacant houses available. All houses are occupied in this building $buildingName.")
        else {
            createHeaderExcelCell(row, 0, "Vacant Houses")
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "House")
            createHeaderExcelCell(row, 1, "Vacant in Days")
            createHeaderExcelCell(row, 2, "Vacant info")
            for (house in vacantHousesList) {
                row = sheet.createRow(rowIndex++)
                val nameCell = createExcelCell(row, 0, house.name)
                val vacantDays = house.vacantDays()
                createExcelCell(row, 1, vacantDays)
                createExcelCell(row, 2, HouseActions.vacantDaysText(vacantDays))
                addCommentToExcelCell(sheet, nameCell, house.notes)
            }
            sheet.createRow(rowIndex++) // empty row
            sheet.createRow(rowIndex++) // empty row
        }

        row = sheet.createRow(rowIndex++)
        val rentLatePayersList = rentReportsFragment.rentLatePayersList()
        if (rentLatePayersList.isEmpty()) createHeaderExcelCell(row, 0, "No rent late payers available. All rents had been paid on time in this building $buildingName so far.")
        else {
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "Rent Late Payers")
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "Tenant")
            createHeaderExcelCell(row, 1, "House")
            createHeaderExcelCell(row, 2, "Paid On")
            createHeaderExcelCell(row, 3, "Delay in Days")
            for (rent in rentLatePayersList) {
                row = sheet.createRow(rowIndex++)
                createExcelCell(row, 0, rent.tName)
                val nameCell = createExcelCell(row, 1, rent.hName)
                createExcelCell(row, 2, rent.paidOn)
                createExcelCell(row, 3, rent.delay)
                addCommentToExcelCell(sheet, nameCell, rent.notes)
            }
        }

        autoAdjustRowsColumnsHeight(sheet)

        return sheet
    }
}
    /* Inflate menu based on selected tab
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_building_house_reports, menu)
        // Dynamically update the menu based on the selected tab
        menu?.let {
            if (selectedTabPosition == 0) {
                // Repairs tab selected, modify menu for this tab
                menu.findItem(R.id.action_rents).isVisible = false
                menu.findItem(R.id.action_all_repairs).isVisible = true
                menu.findItem(R.id.action_not_closed_repairs).isVisible = true
            } else {
                // Rent tab selected, modify menu for this tab
                menu.findItem(R.id.action_rents).isVisible = true
                menu.findItem(R.id.action_all_repairs).isVisible = false
                menu.findItem(R.id.action_not_closed_repairs).isVisible = false
            }
        }
        return true
    }

    // Handle menu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_rents -> {
                // Handle Settings action
                true
            }
            R.id.action_all_repairs -> {
                repairReportsFragment.onToggleRepairs(true)
                true
            }
            R.id.action_not_closed_repairs -> {
                repairReportsFragment.onToggleRepairs(false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    } */

