package com.android.hms.ui.fragments

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.hms.R
import com.android.hms.ui.activities.BaseActivity
import com.android.hms.ui.activities.DepositNotPaidReportActivity
import com.android.hms.ui.activities.RentNotPaidReportActivity
import com.android.hms.ui.activities.VacantHousesReportActivity
import com.android.hms.ui.activities.RepairReportType
import com.android.hms.ui.activities.RentLatePayersReportActivity
import com.android.hms.ui.adapters.DashboardAdapter
import com.android.hms.ui.adapters.DashboardItem
import com.android.hms.utils.LaunchUtils

enum class DashboardType { DEPOSIT_NOT_PAID_TENANTS, RENT_NOT_PAID_TENANTS, VACANT_HOUSES, RENT_LATE_PAYERS,
    REPAIRS_OPEN_FOR_OVER_WEEK, REPAIRS_NOT_PAID_FOR_OVER_WEEK, REPAIRS_NOT_FIXED, REPAIRS_FIXED_BUT_NOT_PAID, REPAIRS_PAID_BUT_NOT_CLOSED, REPAIRS_ALL_NOT_CLOSED }

class DashboardFragment: BaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private var dashboardAdapter: DashboardAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_list, container, false)

        (activity as? BaseActivity)?.setActionBarView(getString(R.string.app_name))

        recyclerView = view.findViewById(R.id.dashboardRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context) // GridLayoutManager(context, columnCount)

        // Dashboard items
        val dashboardItems = listOf(
            DashboardItem(DashboardType.DEPOSIT_NOT_PAID_TENANTS,"1. Security Deposit Not Paid Tenants", "View tenants who have not paid security deposit so far"),
            DashboardItem(DashboardType.RENT_NOT_PAID_TENANTS, "2. Rent Not Paid Tenants", "View tenants who have not paid rent for this month"),
            DashboardItem(DashboardType.VACANT_HOUSES,"3. Vacant Houses", "View all vacant houses"),
            DashboardItem(DashboardType.RENT_LATE_PAYERS,"4. Rent Late Payers", "View rent late payers so far"),

            DashboardItem(DashboardType.REPAIRS_OPEN_FOR_OVER_WEEK,"5. Repairs Open Over Week", "View repairs that aren open for over a week"),
            DashboardItem(DashboardType.REPAIRS_NOT_PAID_FOR_OVER_WEEK, "6. Repairs Not Paid Over Week", "View repairs note paid for over a week"),
            DashboardItem(DashboardType.REPAIRS_NOT_FIXED,"7. Repairs Not Fixed", "View repairs that are not fixed yet"),
            DashboardItem(DashboardType.REPAIRS_FIXED_BUT_NOT_PAID, "8. Repairs Fixed But Not Paid", "View repairs that are fixed but not paid yet"),
            DashboardItem( DashboardType.REPAIRS_PAID_BUT_NOT_CLOSED, "9. Repairs Paid But Not Closed", "View repairs that are paid but not closed yet"),
            DashboardItem(DashboardType.REPAIRS_ALL_NOT_CLOSED, "10. All Repairs Not Closed", "View all repairs that are not closed yet")
        )

        dashboardAdapter = DashboardAdapter(dashboardItems) { queryType -> handleDashboardItemClick(queryType) }
        recyclerView.adapter = dashboardAdapter

        return view
    }

    override fun filter(searchText: String) {
        dashboardAdapter?.filter(searchText)
    }

    private fun handleDashboardItemClick(dashboardType: DashboardType) {
        val context = context ?: return
        when (dashboardType) {
            DashboardType.DEPOSIT_NOT_PAID_TENANTS -> startActivity(Intent(context, DepositNotPaidReportActivity::class.java))
            DashboardType.RENT_NOT_PAID_TENANTS -> startActivity(Intent(context, RentNotPaidReportActivity::class.java))
            DashboardType.VACANT_HOUSES -> startActivity(Intent(context, VacantHousesReportActivity::class.java))
            DashboardType.RENT_LATE_PAYERS -> startActivity(Intent(context, RentLatePayersReportActivity::class.java))

            DashboardType.REPAIRS_OPEN_FOR_OVER_WEEK -> LaunchUtils.showRepairsReportActivity(context, RepairReportType.OPEN_OVER_WEEK)
            DashboardType.REPAIRS_NOT_PAID_FOR_OVER_WEEK -> LaunchUtils.showRepairsReportActivity(context, RepairReportType.NOT_PAID_OVER_WEEK)
            DashboardType.REPAIRS_NOT_FIXED -> LaunchUtils.showRepairsReportActivity(context, RepairReportType.NOT_FIXED)
            DashboardType.REPAIRS_FIXED_BUT_NOT_PAID -> LaunchUtils.showRepairsReportActivity(context, RepairReportType.FIXED_BUT_NOT_PAID)
            DashboardType.REPAIRS_PAID_BUT_NOT_CLOSED -> LaunchUtils.showRepairsReportActivity(context, RepairReportType.PAID_BUT_NOT_CLOSED)
            DashboardType.REPAIRS_ALL_NOT_CLOSED -> LaunchUtils.showRepairsReportActivity(context, RepairReportType.ALL_NOT_CLOSED)
        }
    }
}