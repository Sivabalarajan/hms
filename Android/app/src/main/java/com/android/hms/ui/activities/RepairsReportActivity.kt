package com.android.hms.ui.activities

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.hms.databinding.ActivityCommonListBinding
import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.android.hms.ui.adapters.RepairReportAdapter
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

enum class RepairReportType { OPEN_OVER_WEEK, NOT_PAID_OVER_WEEK, NOT_FIXED, FIXED_BUT_NOT_PAID, PAID_BUT_NOT_CLOSED, ALL_NOT_CLOSED, ALL_REPAIRS }

class RepairsReportActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityCommonListBinding
    private var adapter: RepairReportAdapter? = null
    private var reportType : RepairReportType = RepairReportType.ALL_NOT_CLOSED
    private var  repairsList = emptyList<Repair>()

    override fun getReportName(): String { return getReportTitle() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommonListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reportTypeName = intent.getStringExtra("RepairReportType")
        reportType = if (reportTypeName == null) RepairReportType.ALL_NOT_CLOSED else RepairReportType.valueOf(reportTypeName) // Convert the String back to the enum
        binding.description.text = getReportDescription()
        setActionBarView(getReportTitle())
        if (reportTypeName == RepairReportType.ALL_REPAIRS.name) binding.description.visibility = android.view.View.GONE

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val progressBar = MyProgressBar(this)
        lifecycleScope.launch {
            repairsList = withContext(Dispatchers.IO) { getRepairsList() }
            if (repairsList.isEmpty()) CommonUtils.showMessage(context, "No repairs found", "No repairs found in this category.")
            else {
                adapter = RepairReportAdapter(repairsList.toMutableList())
                binding.recyclerView.adapter = adapter
                // initSearchView(binding.searchView)
            }
            progressBar.dismiss()
            initializeObservers()
        }
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    private fun initializeObservers() {
        SharedViewModelSingleton.repairInitiatedEvent.observe(this) { adapter?.updateForAddition(it) }
        SharedViewModelSingleton.repairUpdatedEvent.observe(this) { adapter?.updateForChange(it) }
        SharedViewModelSingleton.repairRemovedEvent.observe(this) { adapter?.updateForRemove(it) }
    }

    private fun getRepairsList(): List<Repair> {
        return when (reportType) { // Use the enum to determine the report type
            RepairReportType.OPEN_OVER_WEEK -> Repairs.getAllOpenOverWeek()
            RepairReportType.NOT_PAID_OVER_WEEK -> Repairs.getAllNotPaidOverWeek()
            RepairReportType.NOT_FIXED -> Repairs.getAllNotFixed()
            RepairReportType.FIXED_BUT_NOT_PAID -> Repairs.getAllFixedButNotPaid()
            RepairReportType.PAID_BUT_NOT_CLOSED -> Repairs.getAllPaidButNotClosed()
            RepairReportType.ALL_NOT_CLOSED -> Repairs.getAllNotClosed()
            RepairReportType.ALL_REPAIRS -> Repairs.getAllFromDb().sortedBy { it.raisedOn }
            else -> Repairs.getAllNotClosed()
        }
    }

    private fun getReportDescription(): String {
        return when (reportType) {
            RepairReportType.OPEN_OVER_WEEK -> "Repairs open (not closed) for more than a week" // "View repairs that aren open for over a week"
            RepairReportType.NOT_PAID_OVER_WEEK -> "Repairs are fixed but not paid for more than a week" // "View repairs note paid for over a week"
            RepairReportType.NOT_FIXED -> "Repairs are yet to be fixed" // "View repairs that are not fixed yet"
            RepairReportType.FIXED_BUT_NOT_PAID -> "Repairs are fixed but yet to be paid" // "View repairs that are fixed but not paid yet"
            RepairReportType.PAID_BUT_NOT_CLOSED -> "Repairs are paid but yet to be closed" // "View repairs that are paid but not closed yet"
            RepairReportType.ALL_NOT_CLOSED -> "All repairs that are not closed yet" // "View all repairs that are not closed yet"
            RepairReportType.ALL_REPAIRS -> "All repairs" // "View all repairs"
            else -> "All repairs that are not closed yet"
        }
    }

    private fun getReportTitle(): String {
        return when (reportType) {
            RepairReportType.OPEN_OVER_WEEK -> "Repairs Open Over Week"
            RepairReportType.NOT_PAID_OVER_WEEK -> "Repairs Not Paid Over Week"
            RepairReportType.NOT_FIXED -> "Repairs Not Fixed"
            RepairReportType.FIXED_BUT_NOT_PAID -> "Repairs Fixed But Not Paid"
            RepairReportType.PAID_BUT_NOT_CLOSED -> "Repairs Paid But Not Closed"
            RepairReportType.ALL_NOT_CLOSED -> "All Not Closed Repairs"
            RepairReportType.ALL_REPAIRS -> "All Repairs"
            else -> "All Not Closed Repairs"
        }
    }

    override fun saveToOutputStream(outputStream: OutputStream) {
        if (repairsList.isEmpty()) {
            launch { CommonUtils.showMessage(context, "No repairs found", "No repairs found in this category.") }
            return
        }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        createRepairSheet(sheet, repairsList)
        // repeat(2) { sheet.createRow(rowIndex++) }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
    }
}