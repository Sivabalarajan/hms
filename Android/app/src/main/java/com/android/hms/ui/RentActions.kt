package com.android.hms.ui

import android.content.Context
import android.content.DialogInterface
import com.android.hms.model.Rent
import com.android.hms.model.Rents
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class RentActions(private val context: Context, private val rent: Rent): CoroutineScope by MainScope() {

    fun removeRent(result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        val alertDialog = CommonUtils.confirmMessage(context, "Remove Rent", "Are you sure you want to remove this rent paid by '${rent.tName}' for the house '${rent.hName}'? Please confirm.", "Remove Rent")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progressBar = MyProgressBar(context, "Removing the rent details. Please wait...")
            CoroutineScope(Dispatchers.IO).launch {
                Rents.delete(rent) { success, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            SharedViewModelSingleton.rentRemovedEvent.postValue(rent)
                            NotificationUtils.rentRemoved(rent)
                            CommonUtils.toastMessage(context, "Rent paid by ${rent.tName} has been removed")
                        } else CommonUtils.showMessage(context, "Not able to remove", "Not able to remove the rent paid by ${rent.tName}. $error")
                        result(success, error)
                        alertDialog.dismiss()
                        progressBar.dismiss()
                    }
                }
            }
        }
    }

    companion object {
        fun getSummaryInfo(rents: List<Rent>): String {
            val infoText = StringBuilder()
            rents.forEach { rent ->
                infoText.append("${rent.hName}: $${rent.amount}, notes: ${rent.notes}\n")
            }
            return infoText.toString()
        }

        fun showSummaryInfo(context: Context, rents: List<Rent>) {
            val infoText = getSummaryInfo(rents)
            if (infoText.isNotEmpty()) CommonUtils.toastMessage(context, infoText)
        }

        fun showInfo(context: Context, rent: Rent) {
            CommonUtils.toastMessage(context, getInfo(rent))
        }

        private fun getInfo(rent: Rent) : String {
            val infoText = StringBuilder()
            infoText.append("Building: ${rent.bName}\n")
            infoText.append("House: ${rent.hName}\n")
            infoText.append("Tenant: ${rent.tName}\n")
            infoText.append("Amount: $${CommonUtils.formatNumToText(rent.amount)}\n")
            infoText.append("Paid On: ${CommonUtils.getFullDayDateFormatText(rent.paidOn)}\n")
            if (rent.delay == 1) infoText.append("With one day delay\n")
            else if (rent.delay > 1) infoText.append("With ${rent.delay} days delay\n")
            if (rent.notes.isNotEmpty()) infoText.append("Notes: ${rent.notes}\n")
            return infoText.toString()
        }
    }
}
