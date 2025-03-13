package com.android.hms.ui.reports

import android.os.Bundle
import android.widget.TableRow
import androidx.lifecycle.lifecycleScope
import com.android.hms.model.Expense
import com.android.hms.model.Expenses
import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.android.hms.ui.ExpenseActions
import com.android.hms.ui.RepairActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import kotlin.collections.mapValues

class ExpensesAndRepairsSummaryReportActivity: SummaryReportsBaseActivity() {

    // private var reportData: Map<String, Map<Pair<String, List<String>>, Map<String, Pair<Double, List<String>>>>> = emptyMap()
    private var reportData: Map<String, Map<String, Map<String, Pair<Double, List<String>>>>> = emptyMap()
//    private var expenseReportInfo: Map<String, Map<Pair<String, List<String>>, Map<String, Pair<Double, List<String>>>>> = emptyMap()
//    private var repairReportInfo: Map<String, Map<Pair<String, List<String>>, Map<String, Pair<Double, List<String>>>>> = emptyMap()
    private var expenseList = ArrayList<Expense>()
    private var repairList = ArrayList<Repair>()

    override fun getReportName(): String {
        return "Expense and Repairs Summary Report"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val progressBar = MyProgressBar(this)
        lifecycleScope.launch(Dispatchers.IO) {
            prepareReportData()
            prepareMonthsList()
            withContext(Dispatchers.Main) {
                if (reportData.isEmpty()) CommonUtils.showMessage(context, "No data found", "No expenses or repairs found. Please start capturing expenses / repairs and try again.")
                else createTableReport()
                progressBar.dismiss()
            }
        }
    }

    private fun prepareExpenseReportData(): Map<String, Map<String, Map<String, Pair<Double, List<String>>>>>{
        expenseList = ArrayList(Expenses.getAllFromDb().filter { it.paidOn > 0 }.sortedBy { it.bName }) // With(compareBy<Expense> { it.paidOn }.thenBy { it.bName }))
        return expenseList
            .groupBy { it.bName }
            .mapValues { (_, buildingExpenses) ->
                buildingExpenses
                    .groupBy { it.category } // .groupBy { Pair(it.category, buildingExpenses.filter { expense -> expense.category == it.category }.map { expense -> expense.notes }) }
                    .mapValues { (_, categoryExpenses) ->
                        categoryExpenses
                            .groupBy { expense -> CommonUtils.getMonthYearOnlyFormatText(expense.paidOn) }
                            .mapValues { (_, monthExpenses) ->
                                Pair(monthExpenses.sumOf { it.amount }, monthExpenses.map { ExpenseActions.getInfo(it) })
                            }
                    }
            }
    }

    private fun prepareRepairReportData(): Map<String, Map<String, Map<String, Pair<Double, List<String>>>>> {
        repairList = ArrayList(Repairs.getAllFromDb().filter { it.paidOn > 0 }.sortedBy { it.bName }) // With(compareBy<Repair> { it.paidOn }.thenBy { it.bName }))
        return repairList
            .groupBy { it.bName }
            .mapValues { (_, buildingRepairs) ->
                buildingRepairs
                    .groupBy { it.desc }// groupBy { Pair(it.desc, buildingRepairs.filter { repair -> repair.desc == it.desc }.map { repair -> repair.notes }) }
                    .mapValues { (_, descRepairs) ->
                        descRepairs
                            .groupBy { repair -> CommonUtils.getMonthYearOnlyFormatText(repair.paidOn) }
                            .mapValues { (_, monthRepairs) ->
                                Pair(monthRepairs.sumOf { it.amount }, monthRepairs.map { RepairActions.getInfo(it) })
                            }
                    }
            }
    }

    private fun prepareReportDataOld() {
        reportData = prepareExpenseReportData() + prepareRepairReportData()
        val sortedOuterMap = reportData.toSortedMap()
        reportData = sortedOuterMap.mapValues { (_, innerMap) ->
            innerMap.toSortedMap(compareBy { it }).toMutableMap()
        }.toMutableMap()
    }

    private fun prepareReportData() {
        val expenseReportData = prepareExpenseReportData()
        val repairReportData = prepareRepairReportData()
        val mergedData = mutableMapOf<String, MutableMap<String, Map<String, Pair<Double, List<String>>>>>()

        // Add all expense data first
        expenseReportData.forEach { (building, categories) -> mergedData[building] = categories.toMutableMap() }

        // Merge repair data, handling potential collisions
        repairReportData.forEach { (building, repairs) ->
            val existingBuildingData = mergedData.getOrPut(building) { mutableMapOf() }
            repairs.forEach { (repairDesc, monthData) -> existingBuildingData[repairDesc] = monthData }
        }

        // Sort the outer map by building name
        val sortedOuterMap = mergedData.toSortedMap()

        // Sort the inner maps by category/description
        reportData = sortedOuterMap.mapValues { (_, innerMap) ->
            innerMap.toSortedMap(compareBy { it }).toMutableMap()
        }.toMutableMap()
    }

    override fun prepareMonthsList() {
        val highestPaidOnExpense = expenseList.maxByOrNull { it.paidOn }?.paidOn ?: return
        val lowestPaidOnExpense = expenseList.minByOrNull { it.paidOn }?.paidOn ?: return
        val highestPaidOnRepair = repairList.maxByOrNull { it.paidOn }?.paidOn ?: return
        val lowestPaidOnRepair = repairList.minByOrNull { it.paidOn }?.paidOn ?: return
        val lowestPaidOn = if (lowestPaidOnRepair < lowestPaidOnExpense) lowestPaidOnRepair else lowestPaidOnExpense
        val highestPaidOn = if (highestPaidOnRepair > highestPaidOnExpense) highestPaidOnRepair else highestPaidOnExpense
        monthsList = CommonUtils.getMonthsBetweenDatesReverse(lowestPaidOn, highestPaidOn)
    }

    private fun createTableReport() {
        val totalPerMonthList = mutableMapOf<String, Double>()
        reportData.forEach { building ->
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
//                    categoryView.setOnClickListener { CommonUtils.toastMessage(context, category.key.second.toString()) }
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
                            val notesList = expense.value.second
                            if (notesList.isNotEmpty()) CommonUtils.toastMessage(context, notesList.joinToString("\n"))
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
        tableRow.addView(createTVHeader("Category / Details"))
        monthsList.forEach { headerText -> tableRow.addView(createTVHeader(headerText)) }
        return tableRow
    }

    override fun saveToOutputStream(outputStream: OutputStream) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0

        val totalPerMonthList = mutableMapOf<String, Double>()
        for ((building, categoryData) in reportData) {
            val buildingRow = sheet.createRow(rowIndex++)
            createHeaderExcelCell(buildingRow, 0, "Building: $building")
            val categoryHeadingRow = sheet.createRow(rowIndex++)
            var monthColumnIndex = 0
            totalPerMonthList.clear()
            createHeaderExcelCell(categoryHeadingRow, monthColumnIndex++, "Category / Details")
            for (month in monthsList) createHeaderExcelCell(categoryHeadingRow, monthColumnIndex++, month)
            for ((category, monthData) in categoryData) {
                val categoryDataRow = sheet.createRow(rowIndex++)
                createExcelCell(categoryDataRow, 0, category)
                // if (category.first.lowercase(Locale.US).contains("insurance")) addCommentToExcelCell(sheet, cell, category.second.toString())
                for ((month, amount) in monthData) {
                    monthColumnIndex = monthsList.indexOfFirst { it == month } + 1
                    val cell = createExcelCell(categoryDataRow, monthColumnIndex, amount.first)
                    totalPerMonthList[month] = totalPerMonthList.getOrDefault(month, 0.0) + amount.first
                    if (amount.second.isNotEmpty()) addCommentToExcelCell(sheet, cell, amount.second.joinToString("\n"))
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
