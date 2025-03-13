package com.android.hms.ui.reports

import android.os.Bundle
import android.widget.TableRow
import androidx.lifecycle.lifecycleScope
import com.android.hms.model.Expense
import com.android.hms.model.Expenses
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.ui.ExpenseActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.util.Locale
import kotlin.collections.mapValues

class ExpensesSummaryReportActivity: SummaryReportsBaseActivity() {

    // private var expenseReportData: Map<String, Map<Pair<String, List<Expense>>, Map<String, Pair<Double, List<Expense>>>>> = emptyMap()
    private var expenseReportData: Map<String, Map<String, Map<String, Pair<Double, List<Expense>>>>> = emptyMap()
    private var expenseList = ArrayList<Expense>()

    override fun getReportName(): String {
        return "Expense Summary Report"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val progressBar = MyProgressBar(this)
        lifecycleScope.launch(Dispatchers.IO) {
            prepareReportData()
            prepareMonthsList()
            withContext(Dispatchers.Main) {
                if (expenseReportData.isEmpty()) CommonUtils.showMessage(context, "No data found", "No expenses found. Please start capturing expenses and try again.")
                else createTableReport()
                progressBar.dismiss()
            }
        }
    }

    private fun prepareReportData() {
        expenseList = ArrayList(Expenses.getAllFromDb().filter { it.paidOn > 0 }.sortedBy { it.bName }) // With(compareBy<Expense> { it.paidOn }.thenBy { it.bName }))
        expenseReportData = expenseList
            .groupBy { it.bName }
            .mapValues { (_, buildingExpenses) ->
                buildingExpenses
                    .groupBy { it.category } // .groupBy { Pair(it.category, buildingExpenses) }
                    .mapValues { (_, categoryExpenses) ->
                        categoryExpenses
                            .groupBy { expense ->
                                CommonUtils.getMonthYearOnlyFormatText(expense.paidOn)
//                                    val calendar = Calendar.getInstance().apply { timeInMillis = expense.raisedOn }
//                                    "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}" // Group by Year-Month
                            }
                            .mapValues { (_, monthExpenses) ->
                                Pair(monthExpenses.sumOf { it.amount }, monthExpenses)
                            }
                    }
            }
        sortReportData()
    }

    private fun sortReportData() {
        val sortedOuterMap = expenseReportData.toSortedMap()
        expenseReportData = sortedOuterMap.mapValues { (_, innerMap) ->
            innerMap.toSortedMap(compareBy { it }).toMutableMap()
        }.toMutableMap()
    }

    override fun prepareMonthsList() {
        val lowestPaidOn = expenseList.minByOrNull { it.paidOn }?.paidOn ?: return
        val highestPaidOn = expenseList.maxByOrNull { it.paidOn }?.paidOn ?: return
        monthsList = CommonUtils.getMonthsBetweenDatesReverse(lowestPaidOn, highestPaidOn)
    }

    private fun createTableReport() {
        val totalPerMonthList = mutableMapOf<String, Double>()
        expenseReportData.forEach { building ->
            reportTableLayout.addView(createHeaderTableRow("Building: ${building.key}"))
            reportTableLayout.addView(createHeader())
            val tableTotalRow = TableRow(context)
            val amountTotalViews = createHeaderAmountViews()
            tableTotalRow.addView(createTVHeader("Total"))
            amountTotalViews.forEach { view -> tableTotalRow.addView(view) }
            totalPerMonthList.clear()
            building.value.forEach { category ->
                val tableRow = TableRow(context)
                val categoryView = createTV(category.key)
                tableRow.addView(categoryView)
//                if (category.key.first.lowercase(Locale.US).contains("insurance")) {
//                    highlightWhenTapped(categoryView)
//                    categoryView.setOnClickListener { ExpenseActions.showInfo(context, category.key.second) }
//                }
                val amountViews = createAmountViews()
                amountViews.forEach { view -> tableRow.addView(view) }
                category.value.forEach { expense ->
                    val monthIndex = monthsList.indexOfFirst { it == expense.key }
                    if (monthIndex != -1) {
                        amountViews[monthIndex].text = CommonUtils.formatNumToText(expense.value.first)
                        totalPerMonthList[expense.key] = totalPerMonthList.getOrDefault(expense.key, 0.0) + expense.value.first
                        highlightWhenTapped(amountViews[monthIndex])
                        amountViews[monthIndex].setOnClickListener {
                            val expenses = expense.value.second
                            if (expenses.isNotEmpty()) ExpenseActions.showInfo(context, expenses)
                        }
                    }
                }
                reportTableLayout.addView(tableRow)
            }
            reportTableLayout.addView(createEmptyTextTableRow(monthsList.size + 1))
            totalPerMonthList.forEach { (month, amount) ->
                val monthIndex = monthsList.indexOfFirst { it == month }
                amountTotalViews[monthIndex].text = CommonUtils.formatNumToText(amount)
            }
            reportTableLayout.addView(tableTotalRow)
            reportTableLayout.addView(createEmptyTV()) // empty row
        }
        // repeat(3) { reportTableLayout.addView(createEmptyTV()) } // empty rows
    }

    private fun createHeader(): TableRow {
        val tableRow = TableRow(context)
        tableRow.addView(createTVHeader("Category"))
        monthsList.forEach { headerText -> tableRow.addView(createTVHeader(headerText)) }
        return tableRow
    }

    override fun saveToOutputStream(outputStream: OutputStream) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0

        val totalPerMonthList = mutableMapOf<String, Double>()
        for ((building, categoryData) in expenseReportData) {
            val buildingRow = sheet.createRow(rowIndex++)
            createHeaderExcelCell(buildingRow, 0, "Building: $building")
            val categoryHeadingRow = sheet.createRow(rowIndex++)
            var monthColumnIndex = 0
            totalPerMonthList.clear()
            createHeaderExcelCell(categoryHeadingRow, monthColumnIndex++, "Category")
            for (month in monthsList) createHeaderExcelCell(categoryHeadingRow, monthColumnIndex++, month)
            for ((category, monthData) in categoryData) {
                val categoryDataRow = sheet.createRow(rowIndex++)
                var cell = createExcelCell(categoryDataRow, 0, category)
                // if (category.first.lowercase(Locale.US).contains("insurance")) addCommentToExcelCell(sheet, cell, ExpenseActions.getInfo(category.second))
                for ((month, amount) in monthData) {
                    monthColumnIndex = monthsList.indexOfFirst { it == month } + 1
                    cell = createExcelCell(categoryDataRow, monthColumnIndex, amount.first)
                    totalPerMonthList[month] = totalPerMonthList.getOrDefault(month, 0.0) + amount.first
                    addCommentToExcelCell(sheet, cell, ExpenseActions.getInfo(amount.second))
                }
            }
            sheet.createRow(rowIndex++) // empty row
            val totalRow = sheet.createRow(rowIndex++)
            createHeaderExcelCell(totalRow, 0, "Total")
            totalPerMonthList.forEach { (month, totalAmount) ->
                val monthIndex = monthsList.indexOfFirst { it == month } + 1
                createHeaderExcelCell(totalRow, monthIndex, totalAmount)
            }
            repeat(2) { sheet.createRow(rowIndex++) }
        }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
        // CommonUtils.toastMessage(context, "Report exported successfully.")
        // ExportReport.exportExpenseReportToExcel(expenseReportData, outputStream)
    }
}
