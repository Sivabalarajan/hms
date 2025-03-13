package com.android.hms.ui

import android.content.Context
import android.content.DialogInterface
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class HouseActions(private val context: Context, private val house: House): CoroutineScope by MainScope() {

    fun select() {
        LaunchUtils.showHouseReportsActivity(context, house)
    }

    fun markDepositAsPaid(result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        if (house.tName.isEmpty()) {
            CommonUtils.showMessage(context, "Tenant is not assigned", "House is vacant. Please assign tenant first.")
            return
        }
        if (house.dPaid) {
            CommonUtils.toastMessage(context, "Security deposit is already paid for this house ${house.name}")
            return
        }
        val alertDialog = CommonUtils.confirmMessage(context, "Mark Deposit as Paid", "Are you sure ${house.tName} has paid the security deposit for this house '${house.name}'. Please confirm.", "Mark Deposit as Paid")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progressBar = MyProgressBar(context, "Marking the security deposit as paid. Please wait...")
            CoroutineScope(Dispatchers.IO).launch {
                house.dPaid = true
                Houses.update(house) { success, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            SharedViewModelSingleton.houseUpdatedEvent.postValue(house)
                            result(true, "")
                            CommonUtils.toastMessage(context, "House ${house.name} is marked as security deposit paid")
                            NotificationUtils.houseRemoved(house)
                        } else {
                            CommonUtils.showMessage(context, "Not able to mark as paid", "Not able to mark security deposit as paid for this house. $error")
                            result(false, error)
                        }
                        alertDialog.dismiss()
                        progressBar.dismiss()
                    }
                }
            }
        }
    }

    fun removeHouse(result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        val alertDialog = CommonUtils.confirmMessage(context, "Remove House", "Are you sure you want to remove this '${house.name}' house?", "Remove House")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progressBar = MyProgressBar(context, "Deleting house details. Please wait...")
            CoroutineScope(Dispatchers.IO).launch {
                Houses.delete(house) { success, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            SharedViewModelSingleton.houseRemovedEvent.postValue(house)
                            result(true, "")
                            CommonUtils.toastMessage(context, "House ${house.name} is removed")
                            NotificationUtils.houseRemoved(house)
                        } else {
                            CommonUtils.showMessage(context, "Not able to remove", "Not able to remove the house. $error")
                            result(false, error)
                        }
                        alertDialog.dismiss()
                        progressBar.dismiss()
                    }
                }
            }
        }
    }

    fun initiateRepair() {
        LaunchUtils.showRepairActivity(context, house.bId, house.bName, house.id)
    }

    fun submitExpense() {
        LaunchUtils.showExpenseActivity(context, house.bId, house.bName, house.id)
    }

    fun assignTenant() {
        LaunchUtils.showAssignTenantActivity(context, house)
    }

    fun getRentPaid() {
        LaunchUtils.showPayHouseRentActivity(context, house)
    }

    fun makeHouseVacant() {
        LaunchUtils.showMakeHouseVacantActivity(context, house)
    }

    fun showInfo() {
        val infoText = getInfo()
        if (infoText.isNotEmpty()) CommonUtils.toastMessage(context, infoText)
    }

    fun getInfo(): String {
        val infoText = StringBuilder()
        if (house.tId.isEmpty()) infoText.append("${vacantDaysText(house.vacantDays())}\n")
        else {
            infoText.append("Occupied by ${house.tName}, since ${CommonUtils.getFullDayDateFormatText(house.tJoined)}.\n")
            infoText.append("${depositPendingDaysText(house.depositPendingDays())}\n")
            infoText.append("${rentPendingDaysText(house.rentPendingDays())}\n")
        }
        if (house.notes.isNotEmpty()) infoText.append("Notes: ${house.notes}\n")
        return infoText.toString()
    }

    companion object {

        fun showInfo(context: Context, id: String) {
            val house = Houses.getById(id) ?: return
            HouseActions(context, house).showInfo()
        }

        fun getInfo(context: Context, id: String): String {
            val house = Houses.getById(id) ?: return ""
            return HouseActions(context, house).getInfo()
        }

        fun depositPendingDaysText(pendingDays: Int): String {
            return when {
                pendingDays < -1 -> "Deposit is not pending"
                pendingDays == -1 -> "Tenant to be assigned"
                pendingDays == 0 -> "Security deposit amount is paid"
                pendingDays == 1 -> "To be paid today"
                pendingDays == 2 -> "Security deposit is pending from yesterday"
                else -> "Security deposit is pending for ${CommonUtils.formatNumToText(pendingDays)} days"
            }
        }

        fun rentPendingDaysText(pendingDays: Int): String {
            return when {
                pendingDays < -1 -> "Rent is not pending"
                pendingDays == -1 -> "Tenant to be assigned"
                pendingDays == 0 -> "Waiting for first rent to be paid"
                pendingDays == 1 -> "Rent pending from today"
                pendingDays == 2 -> "Rent pending from yesterday"
                else -> "Rent pending for ${CommonUtils.formatNumToText(pendingDays)} days"
            }
        }

        fun vacantDaysText(vacantDays: Int): String {
            return when {
                vacantDays < 0  -> "Tenant to be assigned"
                vacantDays == 0 -> "Vacant from today"
                vacantDays == 1 -> "Vacant from yesterday"
                else -> "Vacant for ${CommonUtils.formatNumToText(vacantDays)} days"
            }
        }
    }
}