package com.android.hms.utils

import android.content.Context
import android.content.Intent
import com.android.hms.model.Expense
import com.android.hms.model.Repair
import com.android.hms.model.House
import com.android.hms.ui.activities.OwnerMainActivity
import com.android.hms.model.Users
import com.android.hms.ui.activities.AddBuildingActivity
import com.android.hms.ui.activities.AddHouseActivity
import com.android.hms.ui.activities.AssignTenantActivity
import com.android.hms.ui.activities.BuildingReportsActivity
import com.android.hms.ui.activities.ExpenseActivity
import com.android.hms.ui.activities.RepairReportType
import com.android.hms.ui.activities.RepairsReportActivity
import com.android.hms.ui.activities.HouseReportsActivity
import com.android.hms.ui.activities.MakeHouseVacantActivity
import com.android.hms.ui.activities.PayHouseRentActivity
import com.android.hms.ui.activities.RepairActivity
import com.android.hms.ui.activities.TenantMainActivity
import com.android.hms.viewmodel.SharedViewModelSingleton

object LaunchUtils {

    fun showMainActivity(context: Context) {
        val activityClass = when {
            Users.isCurrentUserAdmin() -> OwnerMainActivity::class.java
            Users.isCurrentUserOwner() -> OwnerMainActivity::class.java
            Users.isCurrentUserTenant() -> TenantMainActivity::class.java
//            Users.isCurrentUserHelper() -> HelperMainActivity::class.java
            else -> OwnerMainActivity::class.java
        }
        context.startActivity(Intent(context, activityClass))
    }

    fun showBuildingActivity(context: Context, id: String = "") {
        val intent = Intent(context, AddBuildingActivity::class.java)
        intent.putExtra(Globals.gFieldId, id)
        context.startActivity(intent)
    }

    fun showHouseActivity(context: Context, house: House) {
        showHouseActivity(context, house.bId, house.bName, house.id)
    }

    fun showHouseActivity(context: Context, buildingId: String, buildingName: String, id: String = "") {
        if (buildingId.isEmpty() || buildingName.isEmpty()) {
            CommonUtils.showMessage(context, "Building - House", "Not able to associate with any building. Please try again later.")
            return
        }
        val intent = Intent(context, AddHouseActivity::class.java)
        intent.putExtra(Globals.gFieldId, id)
        intent.putExtra("buildingId", buildingId)
        intent.putExtra("buildingName", buildingName)
        context.startActivity(intent)
    }

    fun showAssignTenantActivity(context: Context, house: House) {
        if (house.tId.isNotEmpty()) {
            CommonUtils.showMessage(context, "House is not vacant", "House has been occupied by a tenant already.")
            return
        }
        SharedViewModelSingleton.currentHouseObject = house
        context.startActivity(Intent(context, AssignTenantActivity::class.java))
    }

    fun showPayHouseRentActivity(context: Context, house: House) {
        if (house.tId.isEmpty()) {
            CommonUtils.showMessage(context, "House is vacant", "House is vacant. Please assign tenant before paying the rent.")
            return
        }
        SharedViewModelSingleton.currentHouseObject = house
        context.startActivity(Intent(context, PayHouseRentActivity::class.java))
    }

    fun showMakeHouseVacantActivity(context: Context, house: House) {
        if (house.tId.isEmpty()) {
            CommonUtils.showMessage(context, "House is vacant", "House is already vacant.")
            return
        }
        SharedViewModelSingleton.currentHouseObject = house
        context.startActivity(Intent(context, MakeHouseVacantActivity::class.java))
    }

    fun showRepairActivity(context: Context, repair: Repair) {
        SharedViewModelSingleton.currentRepairObject = repair
        showRepairActivity(context, repair.bId, repair.bName, repair.hId, repair.id)
    }

    fun showRepairActivity(context: Context, buildingId: String, buildingName: String, houseId: String = "", repairId: String = "") {
        if (buildingId.isEmpty() || buildingName.isEmpty()) {
            CommonUtils.showMessage(context, "Building", "Not able to get the building details. Please try again later.")
            return
        }
        val intent = Intent(context, RepairActivity::class.java)
        intent.putExtra(Globals.gFieldId, repairId)
        intent.putExtra("buildingId", buildingId)
        intent.putExtra("buildingName", buildingName)
        intent.putExtra("houseId", houseId)
        context.startActivity(intent)
    }

    fun showExpenseActivity(context: Context, expense: Expense) {
        SharedViewModelSingleton.currentExpenseObject = expense
        showExpenseActivity(context, expense.bId, expense.bName, expense.hId, expense.id)
    }

    fun showExpenseActivity(context: Context, buildingId: String, buildingName: String, houseId: String = "", expenseId: String = "") {
        if (buildingId.isEmpty() || buildingName.isEmpty()) {
            CommonUtils.showMessage(context, "Building", "Not able to get the building details. Please try again later.")
            return
        }
        val intent = Intent(context, ExpenseActivity::class.java)
        intent.putExtra(Globals.gFieldId, expenseId)
        intent.putExtra("buildingId", buildingId)
        intent.putExtra("buildingName", buildingName)
        intent.putExtra("houseId", houseId)
        context.startActivity(intent)
    }

    fun showBuildingReportsActivity(context: Context, buildingId: String, buildingName: String) {
        if (buildingId.isEmpty() || buildingName.isEmpty()) {
            CommonUtils.showMessage(context, "Building", "Not able to get the building details. Please try again later.")
            return
        }
        val intent = Intent(context, BuildingReportsActivity::class.java)
        intent.putExtra("buildingId", buildingId)
        intent.putExtra("buildingName", buildingName)
        context.startActivity(intent)
    }

    fun showHouseReportsActivity(context: Context, house: House) {
        SharedViewModelSingleton.currentHouseObject = house
        context.startActivity(Intent(context, HouseReportsActivity::class.java))
    }

    fun showRepairsReportActivity(context: Context, repairReportType: RepairReportType) {
        val intent = Intent(context, RepairsReportActivity::class.java)
        intent.putExtra("RepairReportType", repairReportType.name) // Pass the enum's name as a String
        context.startActivity(intent)
    }
}