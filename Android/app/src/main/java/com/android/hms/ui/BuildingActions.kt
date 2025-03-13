package com.android.hms.ui

import android.content.Context
import android.content.DialogInterface
import com.android.hms.model.Building
import com.android.hms.model.Buildings
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class BuildingActions(private val context: Context, private val buildingId: String, private val buildingName: String):  CoroutineScope by MainScope() {

    fun select() {
        LaunchUtils.showBuildingReportsActivity(context, buildingId, buildingName)
    }

    fun initiateRepair() {
        LaunchUtils.showRepairActivity(context, buildingId, buildingName)
    }

    companion object {

        fun removeBuilding(context: Context, building: Building, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
            val alertDialog = CommonUtils.confirmMessage(context, "Remove building and its houses", "Are you sure you want to remove this '${building.name}' building?\n\n\nThis will also remove the houses associated with this building.\n\n\nPlease confirm.", "Remove Building and Houses")
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val progressBar = MyProgressBar(context, "Removing the building details. Please wait...")
                CoroutineScope(Dispatchers.IO).launch {
                    Buildings.delete(building) { success, error ->
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                SharedViewModelSingleton.buildingRemovedEvent.postValue(building)
                                NotificationUtils.buildingRemoved(building)
                                CommonUtils.toastMessage(context, "Building ${building.name} is removed")
                            } else CommonUtils.showMessage(context, "Not able to remove", "Not able to remove the building. $error")
                            result(success, error)
                            alertDialog.dismiss()
                            progressBar.dismiss()
                        }
                    }
                }
            }
        }
    }
}