package com.android.hms.ui.activities

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.hms.databinding.ActivityCommonListBinding
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.ui.HouseActions
import com.android.hms.ui.adapters.RentNotPaidAdapter
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class RentNotPaidReportActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityCommonListBinding
    private var adapter: RentNotPaidAdapter? = null
    private var rentDefaulters = ArrayList<House>()

    override fun getReportName(): String { return "Rent Not Paid Tenants" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommonListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.description.text = "View tenants who have not paid rent for this month"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val progressBar = MyProgressBar(this)
        lifecycleScope.launch {
            rentDefaulters = Houses.getRentNotPaidTenants()
            if (rentDefaulters.isEmpty()) CommonUtils.showMessage(context, "No rent defaulters", "No rent defaulters found at this point of time.")
            else {
                adapter = RentNotPaidAdapter(rentDefaulters)
                binding.recyclerView.adapter = adapter
                // initSearchView(binding.searchView)
            }
            progressBar.dismiss()
        }
        setActionBarView(getReportName())
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    override fun saveToOutputStream(outputStream: OutputStream) {
        if (rentDefaulters.isEmpty()) {
            launch { CommonUtils.showMessage(context, "No rent defaulters", "No rent defaulters found at this point of time.") }
            return
        }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0
        var row = sheet.createRow(rowIndex++)
        if (rentDefaulters.isEmpty()) createHeaderExcelCell(row, 0, "No rent not paid details available. All rents are paid.")
        else {
            createHeaderExcelCell(row, 0, getReportName())
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "Building")
            createHeaderExcelCell(row, 1, "Tenant")
            createHeaderExcelCell(row, 2, "House")
            createHeaderExcelCell(row, 3, "Days Pending")
            createHeaderExcelCell(row, 4, "Days Pending Info")
            for (house in rentDefaulters) {
                row = sheet.createRow(rowIndex++)
                createExcelCell(row, 0, house.tName)
                createExcelCell(row, 1, house.bName)
                val nameCell = createExcelCell(row, 2, house.name)
                val rentPendingDays = house.rentPendingDays()
                createExcelCell(row, 3, rentPendingDays)
                createExcelCell(row, 4, HouseActions.rentPendingDaysText(rentPendingDays))
                addCommentToExcelCell(sheet, nameCell, house.notes)
            }
        }

        // repeat(2) { sheet.createRow(rowIndex++) }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
    }
}