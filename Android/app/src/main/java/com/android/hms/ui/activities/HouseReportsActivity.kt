package com.android.hms.ui.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.hms.databinding.ActivityHouseReportsBinding
import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.android.hms.model.Rents
import com.android.hms.ui.HouseActions
import com.android.hms.ui.adapters.RepairReportAdapter
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class HouseReportsActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityHouseReportsBinding
    private var adapter: RepairReportAdapter? = null
    private val house = SharedViewModelSingleton.currentHouseObject
    private var notClosedRepairs = ArrayList<Repair>()

    override fun getReportName(): String { return "House ${house?.name} Report" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHouseReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)  // setContentView(R.layout.activity_building_house_reports)

        setActionBarView(getReportName())

        if (house == null) {
            CommonUtils.showMessage(context, "House", "Not able to get the house details. Please try again later.")
            return
        }
        SharedViewModelSingleton.currentHouseObject = null

        initViews()
    }

    private fun initViews() {
        val house = house
        if (house == null) {
            CommonUtils.showMessage(context, "House", "Not able to get the house details. Please try again later.")
            return
        }
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch {
            binding.tvHouseInfo.text = "${house.name} in ${house.bName}"
            binding.tvHouseInfo.setOnClickListener { LaunchUtils.showHouseActivity(context, house) }

            binding.tvHouseVacant.text = if (house.tId.isEmpty()) {
                binding.tvHouseDepositPaid.text = "Deposit paid: N / A"
                binding.tvHouseRentPaid.text = "Rent paid: N / A"
                HouseActions.vacantDaysText(house.vacantDays())
            }
            else {
                binding.tvHouseDepositPaid.text = HouseActions.depositPendingDaysText(house.depositPendingDays())
                binding.tvHouseRentPaid.text = HouseActions.rentPendingDaysText(house.rentPendingDays())
                "Occupied by ${house.tName}, since ${CommonUtils.getFullDayDateFormatText(house.tJoined)}."
            }

            val latePayersList = withContext(Dispatchers.IO) { Rents.getRentLatePayersByHouse(house.id).groupBy({ it.first }, { it.second }).toSortedMap() }
            val latePayerText = StringBuilder()
            var index = 0
            latePayersList.entries.forEach { entry ->
                if (latePayersList.size > 1)
                    latePayerText.append("${++index}. Tenant ${entry.key} was late by ${CommonUtils.formatIntList(entry.value)} days in paying rent earlier.\n")
                else
                    latePayerText.append("Tenant ${entry.key} was late by ${CommonUtils.formatIntList(entry.value)} days in paying rent earlier.\n")
            }

            binding.tvHouseRentDelay.text = if (latePayerText.isEmpty()) "No late rent payment info available.\n" else latePayerText.toString()

            notClosedRepairs = Repairs.getAllNotClosedByHouse(house.id)
            if (notClosedRepairs.isEmpty()) {
                binding.tvNotClosedHouseRepairDetails.text = "No open repairs found for this house."
                binding.recyclerViewRepairs.visibility = View.GONE
            }
            else {
                binding.recyclerViewRepairs.layoutManager = LinearLayoutManager(context)
                adapter = RepairReportAdapter(notClosedRepairs)
                binding.recyclerViewRepairs.adapter = adapter
            }

            progressBar.dismiss()
        }
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    override fun saveToOutputStream(outputStream: OutputStream) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0
        createHeaderExcelCell(sheet.createRow(rowIndex++), 0, binding.tvHouseInfo.text.toString())
        repeat(1) { sheet.createRow(rowIndex++) }
        createExcelCell(sheet.createRow(rowIndex++), 0, binding.tvHouseVacant.text.toString())
        repeat(1) { sheet.createRow(rowIndex++) }
        createExcelCell(sheet.createRow(rowIndex++), 0, binding.tvHouseDepositPaid.text.toString())
        repeat(1) { sheet.createRow(rowIndex++) }
        createExcelCell(sheet.createRow(rowIndex++), 0, binding.tvHouseRentPaid.text.toString())
        repeat(1) { sheet.createRow(rowIndex++) }
        createExcelCell(sheet.createRow(rowIndex++), 0, binding.tvHouseRentDelay.text.toString())
        repeat(1) { sheet.createRow(rowIndex++) }
        createExcelCell(sheet.createRow(rowIndex++), 0, binding.tvNotClosedHouseRepairDetails.text.toString())
        if (notClosedRepairs.isNotEmpty()) createRepairSheet(sheet, notClosedRepairs, rowIndex)
        // repeat(2) { sheet.createRow(rowIndex++) }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
    }
}