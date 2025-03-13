package com.android.hms.ui.reports

import android.os.Bundle
import android.widget.TableRow
import androidx.lifecycle.lifecycleScope
import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.ui.RepairActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import kotlin.collections.mapValues

class RepairsSummaryReportActivity: SummaryReportsBaseActivity() {

    // private var repairReportData: Map<String, Map<Pair<String, List<Repair>>, Map<String, Pair<Double, List<Repair>>>>> = emptyMap()
    private var repairReportData: Map<String, Map<String, Map<String, Pair<Double, List<Repair>>>>> = emptyMap()
    private var repairList = ArrayList<Repair>()

    override fun getReportName(): String {
        return "Repair Summary Report"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val progressBar = MyProgressBar(this)
        lifecycleScope.launch(Dispatchers.IO) {
            prepareReportData()
            prepareMonthsList()
            withContext(Dispatchers.Main) {
                if (repairReportData.isEmpty()) CommonUtils.showMessage(context, "No data found", "No repairs found. Please start capturing repairs and try again.")
                else {
                    createTableReport()
                    // buildingAdapter = BuildingRepairSummaryReportAdapter(repairReportData)
                    // recyclerView.adapter = buildingAdapter
                }
                progressBar.dismiss()
            }
        }
    }

    private fun prepareReportData() {
        repairList = ArrayList(Repairs.getAllFromDb().filter { it.paidOn > 0 }.sortedBy { it.bName }) // With(compareBy<Repair> { it.paidOn }.thenBy { it.bName }))
        repairReportData = repairList
            .groupBy { it.bName }
            .mapValues { (_, buildingRepairs) ->
                buildingRepairs
                    .groupBy { it.desc } // .groupBy { Pair(it.desc, buildingRepairs) }
                    .mapValues { (_, descRepairs) ->
                        descRepairs
                            .groupBy { repair ->
                                CommonUtils.getMonthYearOnlyFormatText(repair.paidOn)
//                                    val calendar = Calendar.getInstance().apply { timeInMillis = repair.raisedOn }
//                                    "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}" // Group by Year-Month
                            }
                            .mapValues { (_, monthRepairs) ->
                                Pair(monthRepairs.sumOf { it.amount }, monthRepairs)
                            }
                    }
            }
        sortReportData()
    }

    private fun sortReportData() {
        val sortedOuterMap = repairReportData.toSortedMap()
        repairReportData = sortedOuterMap.mapValues { (_, innerMap) ->
            innerMap.toSortedMap(compareBy { it }).toMutableMap()
        }.toMutableMap()
    }

    override fun prepareMonthsList() {
        val lowestPaidOn = repairList.minByOrNull { it.paidOn }?.paidOn ?: return
        val highestPaidOn = repairList.maxByOrNull { it.paidOn }?.paidOn ?: return
        monthsList = CommonUtils.getMonthsBetweenDatesReverse(lowestPaidOn, highestPaidOn)
    }

    private fun createTableReport() {
        val totalPerMonthList = mutableMapOf<String, Double>()
        repairReportData.forEach { building ->
            reportTableLayout.addView(createHeaderTableRow("Building: ${building.key}"))
            reportTableLayout.addView(createHeader())
            val tableTotalRow = TableRow(context)
            val amountTotalViews = createHeaderAmountViews()
            tableTotalRow.addView(createTVHeader("Total"))
            amountTotalViews.forEach { view -> tableTotalRow.addView(view) }
            totalPerMonthList.clear()
            building.value.forEach { description ->
                val tableRow = TableRow(context)
                val descriptionView = createTV(description.key)
                tableRow.addView(descriptionView)
//                if (description.key.first.lowercase(Locale.US).contains("insurance")) {
//                    highlightWhenTapped(descriptionView)
//                    descriptionView.setOnClickListener { RepairActions.showInfo(context, description.key.second) }
//                }
                val amountViews = createAmountViews()
                amountViews.forEach { view -> tableRow.addView(view) }
                description.value.forEach { repair ->
                    val monthIndex = monthsList.indexOfFirst { it == repair.key }
                    if (monthIndex != -1) {
                        amountViews[monthIndex].text = CommonUtils.formatNumToText(repair.value.first)
                        totalPerMonthList[repair.key] = totalPerMonthList.getOrDefault(repair.key, 0.0) + repair.value.first
                        highlightWhenTapped(amountViews[monthIndex])
                        amountViews[monthIndex].setOnClickListener {
                            val repairs = repair.value.second
                            if (repairs.isNotEmpty()) RepairActions.showInfo(context, repairs)
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
        tableRow.addView(createTVHeader("Description"))
        monthsList.forEach { headerText -> tableRow.addView(createTVHeader(headerText)) }
        return tableRow
    }

    override fun saveToOutputStream(outputStream: OutputStream) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0

        val totalPerMonthList = mutableMapOf<String, Double>()
        for ((building, descriptionData) in repairReportData) {
            val buildingRow = sheet.createRow(rowIndex++)
            createHeaderExcelCell(buildingRow, 0, "Building: $building")
            val descriptonHeadingRow = sheet.createRow(rowIndex++)
            var monthColumnIndex = 0
            totalPerMonthList.clear()
            createHeaderExcelCell(descriptonHeadingRow, monthColumnIndex++, "Description")
            for (month in monthsList) createHeaderExcelCell(descriptonHeadingRow, monthColumnIndex++, month)
            for ((description, monthData) in descriptionData) {
                val descriptionDataRow = sheet.createRow(rowIndex++)
                createExcelCell(descriptionDataRow, 0, description)
                // if (description.first.lowercase(Locale.US).contains("insurance")) addCommentToExcelCell(sheet, cell, RepairActions.getInfo(description.second))
                for ((month, amount) in monthData) {
                    monthColumnIndex = monthsList.indexOfFirst { it == month } + 1
                    val cell = createExcelCell(descriptionDataRow, monthColumnIndex, amount.first)
                    totalPerMonthList[month] = totalPerMonthList.getOrDefault(month, 0.0) + amount.first
                    addCommentToExcelCell(sheet, cell, RepairActions.getInfo(amount.second))
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
        // ExportReport.exportRepairReportToExcel(repairReportData, outputStream)
    }
}
