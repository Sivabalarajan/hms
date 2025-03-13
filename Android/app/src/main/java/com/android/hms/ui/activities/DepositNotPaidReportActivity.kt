package com.android.hms.ui.activities

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.hms.databinding.ActivityCommonListBinding
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.ui.HouseActions
import com.android.hms.ui.adapters.DepositNotPaidAdapter
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class DepositNotPaidReportActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityCommonListBinding
    private var adapter: DepositNotPaidAdapter? = null
    private var depositDefaulters = ArrayList<House>()

    override fun getReportName(): String { return "Security Deposit Not Paid Tenants" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommonListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.description.text = "View tenants who have not paid security deposit so far"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val progressBar = MyProgressBar(this)
        lifecycleScope.launch {
            depositDefaulters = Houses.getDepositNotPaidTenants()
            if (depositDefaulters.isEmpty()) CommonUtils.showMessage(context, "No deposit defaulters", "No security deposit defaulters found at this point of time.")
            else {
                adapter = DepositNotPaidAdapter(depositDefaulters)
                binding.recyclerView.adapter = adapter
                // initSearchView(binding.searchView)
            }
            progressBar.dismiss()
        }
        setActionBarView(getReportName())
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    override fun saveToOutputStream(outputStream: OutputStream) {
        if (depositDefaulters.isEmpty()) {
            launch { CommonUtils.showMessage(context, "No deposit defaulters", "No security deposit defaulters found at this point of time.") }
            return
        }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0
        var row = sheet.createRow(rowIndex++)
        if (depositDefaulters.isEmpty()) createHeaderExcelCell(row, 0, "No security deposit defaulters found at this point of time.")
        else {
            createHeaderExcelCell(row, 0, getReportName())
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "Building")
            createHeaderExcelCell(row, 1, "House")
            createHeaderExcelCell(row, 2, "Tenant")
            createHeaderExcelCell(row, 3, "Pending in Days")
            createHeaderExcelCell(row, 4, "Pending Days Text")
            for (house in depositDefaulters) {
                row = sheet.createRow(rowIndex++)
                createExcelCell(row, 0, house.bName)
                val nameCell = createExcelCell(row, 1, house.name)
                createExcelCell(row, 2, house.tName)
                val depositPendingDays = house.depositPendingDays()
                createExcelCell(row, 3, depositPendingDays)
                createExcelCell(row, 4, HouseActions.depositPendingDaysText(depositPendingDays))
                addCommentToExcelCell(sheet, nameCell, house.notes)
            }
            sheet.createRow(rowIndex++) // empty row
            sheet.createRow(rowIndex) // empty row
        }
        // repeat(2) { sheet.createRow(rowIndex++) }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
    }
}
