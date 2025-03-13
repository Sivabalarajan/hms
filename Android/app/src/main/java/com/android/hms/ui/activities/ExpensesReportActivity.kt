package com.android.hms.ui.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.hms.databinding.ActivityCommonListBinding
import com.android.hms.model.Expense
import com.android.hms.model.Expenses
import com.android.hms.ui.adapters.ExpenseReportAdapter
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class ExpensesReportActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityCommonListBinding
    private var adapter: ExpenseReportAdapter? = null
    private var expensesList: MutableList<Expense> = mutableListOf()

    override fun getReportName(): String { return "All Expense Details" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommonListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.description.visibility = View.GONE
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val progressBar = MyProgressBar(this, "Please wait... Loading expense details...")
        lifecycleScope.launch {
            expensesList = withContext(Dispatchers.IO) { Expenses.getAllFromDb().sortedBy { it.paidOn }.toMutableList() }
            if (expensesList.isEmpty()) CommonUtils.showMessage(context, "No expenses", "No expense details found at this point of time.")
            else {
                adapter = ExpenseReportAdapter(expensesList)
                binding.recyclerView.adapter = adapter
                // initSearchView(binding.searchView)
            }
            initializeObservers()
            progressBar.dismiss()
        }
        setActionBarView(getReportName())
    }

    private fun initializeObservers() {
        SharedViewModelSingleton.expenseSubmittedEvent.observe(this) { expense -> updateAdapterForAddition(expense) }
        SharedViewModelSingleton.expenseUpdatedEvent.observe(this) { expense -> updateAdapterForChange(expense) }
        SharedViewModelSingleton.expenseRemovedEvent.observe(this) { expense -> updateAdapterForRemove(expense) }
    }

    private fun updateAdapterForAddition(expense: Expense) {
        if (expensesList.indexOfFirst { it.id == expense.id } != -1) return
        expensesList.add(expense)
        adapter?.notifyItemInserted(expensesList.size) // header row is included
    }

    private fun updateAdapterForChange(expense: Expense) {
        val index = expensesList.indexOfFirst { it.id == expense.id }
        if (index == -1) {
            updateAdapterForAddition(expense)
            return
        }
        expensesList[index] = expense
        adapter?.notifyItemChanged(index + 1) // header row is included
    }

    private fun updateAdapterForRemove(expense: Expense) {
        val index = expensesList.indexOfFirst { it.id == expense.id }
        if (index == -1) return
        expensesList.removeAt(index)
        adapter?.notifyItemRemoved(index + 1) // header row is included
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    override fun saveToOutputStream(outputStream: OutputStream) {
        if (expensesList.isEmpty()) {
            launch { CommonUtils.showMessage(context, "No expenses", "No expense details found at this point of time.") }
            return
        }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0
        val headerRow = sheet.createRow(rowIndex++)
        createHeaderExcelCell(headerRow, 0, "Building")
        createHeaderExcelCell(headerRow, 1, "House")
        createHeaderExcelCell(headerRow, 2, "Category")
        createHeaderExcelCell(headerRow, 3, "Amount")
        createHeaderExcelCell(headerRow, 4, "Payment Type")
        createHeaderExcelCell(headerRow, 5, "Paid On")
        createHeaderExcelCell(headerRow, 6, "Paid By")

        var total = 0.0
        expensesList.forEach { expense ->
            val row = sheet.createRow(rowIndex++)
            createExcelCell(row, 0, expense.bName)
            createExcelCell(row, 1, expense.hName)
            val categoryCell = createExcelCell(row, 2, expense.category)
            createExcelCell(row, 3, expense.amount)
            createExcelCell(row, 4, expense.paidType)
            createExcelCell(row, 5, expense.paidOn)
            createExcelCell(row, 6, expense.paidBy)

            addCommentToExcelCell(sheet, categoryCell, expense.notes)
            total += expense.amount
        }
        sheet.createRow(rowIndex++)
        val totalRow = sheet.createRow(rowIndex++)
        createHeaderExcelCell(totalRow, 0, "Total")
        createHeaderExcelCell(totalRow, 3, total)

        // repeat(2) { sheet.createRow(rowIndex++) }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
    }
}