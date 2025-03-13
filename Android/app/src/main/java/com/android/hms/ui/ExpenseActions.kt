package com.android.hms.ui

import android.content.Context
import android.content.DialogInterface
import com.android.hms.model.Expense
import com.android.hms.model.Expenses
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ExpenseActions(private val context: Context, private val expense: Expense): CoroutineScope by MainScope() {

    fun removeExpense(result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        val alertDialog = CommonUtils.confirmMessage(context, "Remove Expense", "Are you sure you want to remove this expense paid by '${expense.paidBy}' for '${expense.hName.ifEmpty { expense.bName }}'? Please confirm.", "Remove Expense")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progressBar = MyProgressBar(context, "Removing the expense details. Please wait...")
            CoroutineScope(Dispatchers.IO).launch {
                Expenses.delete(expense) { success, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            SharedViewModelSingleton.expenseRemovedEvent.postValue(expense)
                            NotificationUtils.expenseRemoved(expense)
                            CommonUtils.toastMessage(context, "Expense paid by ${expense.category} has been removed")
                        } else CommonUtils.showMessage(context, "Not able to remove", "Not able to remove the expense paid by ${expense.paidBy}. $error")
                        result(success, error)
                        alertDialog.dismiss()
                        progressBar.dismiss()
                    }
                }
            }
        }
    }

    companion object {
        fun getSummaryInfo(expenses: List<Expense>): String {
            val infoText = StringBuilder()
            expenses.forEach { expense ->
                infoText.append("${expense.category} on ${expense.hName.ifEmpty { expense.bName }}: $${expense.amount}, notes: ${expense.notes}\n")
            }
            return infoText.toString()
        }

        fun showSummaryInfo(context: Context, expenses: List<Expense>) {
            val infoText = getSummaryInfo(expenses)
            if (infoText.isNotEmpty()) CommonUtils.toastMessage(context, infoText)
        }

        fun showInfo(context: Context, expense: Expense) {
            CommonUtils.toastMessage(context, getInfo(expense))
        }

        fun getInfo(expense: Expense) : String {
            val infoText = StringBuilder()
            infoText.append("Building: ${expense.bName}\n")
            if (expense.hId.isNotEmpty()) infoText.append("House: ${expense.hName}\n")
            infoText.append("Category: ${expense.category}\n")
            infoText.append("Amount: $${CommonUtils.formatNumToText(expense.amount)}\n")
            infoText.append("Paid On: ${CommonUtils.getFullDayDateFormatText(expense.paidOn)}\n")
            if (expense.paidBy.isNotEmpty()) infoText.append("Paid By: ${expense.paidBy}\n")
            if (expense.notes.isNotEmpty()) infoText.append("Notes: ${expense.notes}\n")
            return infoText.toString()
        }

        fun showInfo(context: Context, expenses: List<Expense>) {
            val infoText = getInfo(expenses)
            if (infoText.isNotEmpty()) CommonUtils.toastMessage(context, infoText)
        }

        fun getInfo(expenses: List<Expense>): String {
            val infoText = StringBuilder()
            expenses.forEach { expense ->
                infoText.append(getInfo(expense))
                infoText.append("\n")
//                if (expense.hName.isEmpty()) infoText.append("${expense.bName}: Category: ${expense.category}, $${CommonUtils.formatNumToText(expense.amount)}, notes: ${expense.notes}\n")
//                else infoText.append("${expense.hName}: Category: ${expense.category}, $${CommonUtils.formatNumToText(expense.amount)}, notes: ${expense.notes}\n")
            }
            return infoText.toString()
        }

    }
}
