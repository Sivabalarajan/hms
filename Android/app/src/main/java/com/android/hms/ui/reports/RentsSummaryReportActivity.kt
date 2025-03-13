package com.android.hms.ui.reports

import android.os.Bundle
import android.widget.TableRow
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Building
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.model.Rent
import com.android.hms.model.Rents
import com.android.hms.ui.HouseActions
import com.android.hms.ui.RentActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class RentsSummaryReportActivity: SummaryReportsBaseActivity() {

    // MutableMap<bName, MutableMap<Pair<hId, hName>, Map<rentPaidMonth, Pair<monthAmount, List<Rent>>>>>
    private var rentReportData: MutableMap<String, MutableMap<Pair<String, String>, Map<String, Pair<Double, List<Rent>>>>> = mutableMapOf()
    private var rentList: ArrayList<Rent> = ArrayList()

    override fun getReportName(): String { return "Rent Summary Report" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val progressBar = MyProgressBar(this)
        lifecycleScope.launch(Dispatchers.IO) {
            prepareReportData()
            prepareMonthsList()
            withContext(Dispatchers.Main) {
                if (rentReportData.isEmpty()) CommonUtils.showMessage(context, "No data found", "No rents data found. Please start capturing rent payment and try again.")
                else {
                    createTableReport()
                    // buildingAdapter = BuildingRentSummaryReportAdapter(rentReportData)
                    // recyclerView.adapter = buildingAdapter
                }
                progressBar.dismiss()
            }
        }
    }

    private fun prepareReportData() {
        rentList = ArrayList(Rents.getAllFromDb().sortedWith(compareBy<Rent> { it.bName }.thenBy { it.hName }))
        rentReportData = rentList
            .groupBy { it.bName }
            .mapValues { (_, buildingRents) ->
                buildingRents
                    .groupBy { Pair(it.hId, it.hName) }
                    .mapValues { (_, houseRents) ->
                        houseRents
                            .groupBy { rent ->
                                CommonUtils.getMonthYearOnlyFormatText(rent.paidOn)
//                                    val calendar = Calendar.getInstance().apply { timeInMillis = rent.paidOn }
//                                    "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}" // Group by Year-Month
                            }
                            .mapValues { (_, monthRents) ->
                                Pair(monthRents.sumOf { it.amount }.toDouble(), monthRents)
                            }
                    }.toMutableMap()
            }.toMutableMap()
        addMissingBuildingsAndHouses()
        sortReportData()
    }

    private fun sortReportData() {
        val sortedOuterMap = rentReportData.toSortedMap()
        // Step 2: Iterate through the outer map and sort the inner map by hName
        rentReportData = sortedOuterMap.mapValues { (_, innerMap) ->
            // Sort the inner map by the second value of Pair<hId, hName>
            innerMap.toSortedMap(compareBy { it.second }).toMutableMap()
        }.toMutableMap()
    }

    private fun addEmptyRentsFromHouses(rentList: MutableList<Rent>, houseList: List<House>) {
        val existingHouseIds = rentList.map { it.hId }.toSet()
        houseList.forEach { house ->
            if (!existingHouseIds.contains(house.id)) {
                val emptyRent = Rent(
                    id = "", tId = "", tName = "", hId = house.id, hName = house.name,
                    amount = 0, bId = house.bId, bName = house.bName, delay = 0, paidOn = 0, notes = ""
                )
                rentList.add(emptyRent)
            }
        }
    }

    private fun addMissingBuildingsAndHouses(buildings: List<Building>, houses: List<House>) {
        buildings.forEach { building ->
            if (!rentReportData.containsKey(building.name)) rentReportData[building.name] = mutableMapOf()
            val houseData = rentReportData[building.name]!!
            houses.filter { it.bId == building.id }.forEach { house ->
                val houseKey = Pair(house.id, house.name)
                if (!houseData.containsKey(houseKey)) houseData[houseKey] = mutableMapOf()
            }
        }
    }

    // Map<bName, Map<Pair<hId, hName>, Map<rentPaidMonth, Pair<monthAmount, List<Rent>>>>>
    private fun addMissingBuildingsAndHouses() {
        val houses = Houses.getAll()
        houses.forEach { house ->
            if (!rentReportData.containsKey(house.bName)) rentReportData[house.bName] = mutableMapOf()
            val houseData = rentReportData[house.bName]
            if (houseData != null) {
                val houseKey = Pair(house.id, house.name)
                if (!houseData.containsKey(houseKey)) houseData[houseKey] = mutableMapOf()
            }
        }
    }

    override fun prepareMonthsList() {
        val lowestPaidOn = rentList.minByOrNull { it.paidOn }?.paidOn ?: return
        val highestPaidOn = rentList.maxByOrNull { it.paidOn }?.paidOn ?: return
        monthsList = CommonUtils.getMonthsBetweenDatesReverse(lowestPaidOn, highestPaidOn)
    }

    private fun createTableReport() {
        val totalPerMonthList = mutableMapOf<String, Double>()
        rentReportData.forEach { building ->
            reportTableLayout.addView(createHeaderTableRow("Building: ${building.key}"))
            reportTableLayout.addView(createHeader())
            val tableTotalRow = TableRow(context)
            val amountTotalViews = createHeaderAmountViews()
            tableTotalRow.addView(createTVHeader("Total"))
            tableTotalRow.addView(createTVHeader(""))
            amountTotalViews.forEach { view -> tableTotalRow.addView(view) }
            totalPerMonthList.clear()
            building.value.forEach { house ->
                val tableRow = TableRow(context)
                val houseNameView = createTV(house.key.second)
                tableRow.addView(houseNameView)
                val houseObject = Houses.getById(house.key.first)
                if (houseObject != null) {
                    highlightWhenTapped(houseNameView)
                    houseNameView.setOnClickListener { HouseActions(context, houseObject).showInfo() }
                    val cellTV = createTV(houseObject.tName.ifEmpty { HouseActions.vacantDaysText(houseObject.vacantDays())})
                    tableRow.addView(cellTV)
                    if (houseObject.tName.isEmpty()) cellTV.setTextColor(context.getColor(R.color.highlight_color))
                }
                else tableRow.addView(createTV("Not able to find Tenant"))
                val amountViews = createAmountViews()
                amountViews.forEach { view -> tableRow.addView(view) }
                house.value.forEach { rent ->
                    val monthIndex = monthsList.indexOfFirst { it == rent.key }
                    if (monthIndex != -1) {
                        amountViews[monthIndex].text = CommonUtils.formatNumToText(rent.value.first)
                        totalPerMonthList[rent.key] = totalPerMonthList.getOrDefault(rent.key, 0.0) + rent.value.first
                        highlightWhenTapped(amountViews[monthIndex])
                        amountViews[monthIndex].setOnClickListener {
                            val rents = rent.value.second
                            if (rents.isNotEmpty()) RentActions.showSummaryInfo(context, rents)
                        }
                    }
                }
                reportTableLayout.addView(tableRow)
            }
            reportTableLayout.addView(createEmptyTextTableRow(monthsList.size + 2))
            totalPerMonthList.forEach { (month, amount) ->
                val monthIndex = monthsList.indexOfFirst { it == month }
                amountTotalViews[monthIndex].text = CommonUtils.formatNumToText(amount)
            }
            reportTableLayout.addView(tableTotalRow)
            reportTableLayout.addView(createEmptyTV()) // empty row
        }
        // repeat(3) { reportTableLayout.addView(createEmptyTV()) } // empty rows
    }

    private fun createHeader() : TableRow {
        val tableRow = TableRow(context)
        tableRow.addView(createTVHeader("House"))
        tableRow.addView(createTVHeader("Tenant"))
        monthsList.forEach { headerText ->  tableRow.addView(createTVHeader(headerText)) }
        return tableRow
    }

    override fun saveToOutputStream(outputStream: OutputStream) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0

        val totalPerMonthList = mutableMapOf<String, Double>()
        for ((building, houseData) in rentReportData) {
            val buildingRow = sheet.createRow(rowIndex++)
            createHeaderExcelCell(buildingRow, 0, "Building: $building")
            val houseHeadingRow = sheet.createRow(rowIndex++)
            var monthColumnIndex = 0
            totalPerMonthList.clear()
            createHeaderExcelCell(houseHeadingRow, monthColumnIndex++, "House")
            createHeaderExcelCell(houseHeadingRow, monthColumnIndex++, "Tenant")
            for (month in monthsList) createHeaderExcelCell(houseHeadingRow, monthColumnIndex++, month)
            for ((house, monthData) in houseData) {
                val houseDataRow = sheet.createRow(rowIndex++)
                var cell = createExcelCell(houseDataRow, 0, house.second)
                addCommentToExcelCell(sheet, cell, HouseActions.getInfo(context, house.first))
                val houseObject = Houses.getById(house.first)
                val cellTenant = createExcelCell(houseDataRow, 1, houseObject?.tName?.ifEmpty { HouseActions.vacantDaysText(houseObject.vacantDays()) } ?: "Not able to find Tenant")
                if (houseObject?.tName?.isEmpty() == true) cellTextColorAsRed(cellTenant)
                for ((month, amount) in monthData) {
                    monthColumnIndex = monthsList.indexOfFirst { it == month } + 2
                    cell = createExcelCell(houseDataRow, monthColumnIndex, amount.first)
                    totalPerMonthList[month] = totalPerMonthList.getOrDefault(month, 0.0) + amount.first
                    addCommentToExcelCell(sheet, cell, RentActions.getSummaryInfo(amount.second))
                }
            }
            sheet.createRow(rowIndex++) // empty row
            val totalRow = sheet.createRow(rowIndex++)
            createHeaderExcelCell(totalRow, 0, "Total")
            totalPerMonthList.forEach { (month, totalAmount) ->
                val monthIndex = monthsList.indexOfFirst { it == month } + 2
                createHeaderExcelCell(totalRow, monthIndex, totalAmount)
            }
            repeat(2) { sheet.createRow(rowIndex++) }
        }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
    }
}