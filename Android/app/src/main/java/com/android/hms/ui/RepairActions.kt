package com.android.hms.ui

import android.content.Context
import android.content.DialogInterface
import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class RepairActions(private val context: Context, private val repair: Repair) : CoroutineScope by MainScope() {

    fun closeRepair(notes: String, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        if (repair.status == Repairs.statuses.last()) {
            CommonUtils.toastMessage(context, "Repair ${repair.desc} is already closed")
            return
        }
        val alertDialog = CommonUtils.confirmMessage(context, "Close Repair", "Are you sure you want to close this '${repair.desc}' repair?", "Close Repair")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progressBar = MyProgressBar(context, "Closing the repair... Please wait...")
            CoroutineScope(Dispatchers.IO).launch {
                repair.notes = notes
                repair.status = Repairs.statuses.last()
                Repairs.update(repair) { success, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            CommonUtils.toastMessage(context, "Repair ${repair.desc} is closed")
                            SharedViewModelSingleton.repairUpdatedEvent.postValue(repair)
                            NotificationUtils.repairClosed(repair)
                        } else CommonUtils.showMessage(context, "Not able to close", "Not able to close the repair. $error")
                        result(success, error)
                        progressBar.dismiss()
                        alertDialog.dismiss()
                    }
                }
            }
        }
    }

    fun removeRepair(result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        val alertDialog = CommonUtils.confirmMessage(context, "Remove Repair", "Are you sure you want to remove this '${repair.desc}' repair?", "Remove Repair")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progressBar = MyProgressBar(context, "Removing the repair details. Please wait...")
            CoroutineScope(Dispatchers.IO).launch {
                Repairs.delete(repair) { success, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            SharedViewModelSingleton.repairRemovedEvent.postValue(repair)
                            NotificationUtils.repairRemoved(repair)
                            CommonUtils.toastMessage(context, "Repair ${repair.desc} is removed")
                        } else CommonUtils.showMessage(context, "Not able to remove", "Not able to remove the repair. $error")
                        result(success, error)
                        alertDialog.dismiss()
                        progressBar.dismiss()
                    }
                }
            }
        }
    }

    fun showInfo() {
        CommonUtils.toastMessage(context, "Raised by: ${repair.raisedBy}\n\nNotes:${repair.notes}")
    }

    companion object {
        fun showInfo(context: Context, repairs: List<Repair>) {
            val infoText = getInfo(repairs)
            if (infoText.isNotEmpty()) CommonUtils.toastMessage(context, infoText)
        }

        fun getInfo(repairs: List<Repair>): String {
            val infoText = StringBuilder()
            repairs.forEach { repair ->
                infoText.append(getInfo(repair))
                infoText.append("\n")
//                if (repair.hName.isEmpty()) infoText.append("${repair.bName}: $${CommonUtils.formatNumToText(repair.amount)}, notes: ${repair.notes}\n")
//                else infoText.append("${repair.hName}: $${CommonUtils.formatNumToText(repair.amount)}, notes: ${repair.notes}\n")
            }
            return infoText.toString()
        }

        fun getInfo(repair: Repair) : String {
            val infoText = StringBuilder()
            infoText.append("Building: ${repair.bName}\n")
            if (repair.hId.isNotEmpty()) infoText.append("House: ${repair.hName}\n")
            infoText.append("Desc: ${repair.desc}\n")
            infoText.append("Amount: $${CommonUtils.formatNumToText(repair.amount)}\n")
            infoText.append("Status: ${repair.status}\n")
            infoText.append("Paid On: ${CommonUtils.getFullDayDateFormatText(repair.paidOn)}\n")
            infoText.append("Tenant Paid: ${CommonUtils.formatBoolToText(repair.tPaid)}\n")
            if (repair.notes.isNotEmpty()) infoText.append("Notes: ${repair.notes}\n")
            return infoText.toString()
        }
    }
}