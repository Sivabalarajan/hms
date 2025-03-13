package com.android.hms.ui.activities

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.hms.databinding.ActivityCommonListBinding
import com.android.hms.model.Rent
import com.android.hms.model.Rents
import com.android.hms.ui.adapters.RentLatePayerAdapter
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class RentLatePayersReportActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityCommonListBinding
    private var adapter: RentLatePayerAdapter? = null
    private var rentLatePayersList = listOf<Rent>()

    override fun getReportName(): String { return "Rent Late Payers" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommonListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.description.text = "View rent late payers so far"
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val progressBar = MyProgressBar(this)
        lifecycleScope.launch {
            rentLatePayersList = Rents.getRentLatePayers()
            if (rentLatePayersList.isEmpty()) CommonUtils.showMessage(context, "No rent late payers", "No rent late payers found at this point of time.")
            else {
                adapter = RentLatePayerAdapter(rentLatePayersList)
                binding.recyclerView.adapter = adapter
                // initSearchView(binding.searchView)
            }
            progressBar.dismiss()
        }
        setActionBarView(getReportName())
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    override fun saveToOutputStream(outputStream: OutputStream) {
        if (rentLatePayersList.isEmpty()) {
            launch { CommonUtils.showMessage(context, "No rent late payers", "No rent late payers available. All rents had been paid on time so far.") }
            return
        }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0
        var row = sheet.createRow(rowIndex++)
        if (rentLatePayersList.isEmpty()) createHeaderExcelCell(row, 0, "No rent late payers available. All rents had been paid on time so far.")
        else {
            createHeaderExcelCell(row, 0, getReportName())
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "Building")
            createHeaderExcelCell(row, 1, "House")
            createHeaderExcelCell(row, 2, "Tenant")
            createHeaderExcelCell(row, 3, "Paid On")
            createHeaderExcelCell(row, 4, "Delay in Days")
            for (rent in rentLatePayersList) {
                row = sheet.createRow(rowIndex++)
                createExcelCell(row, 0, rent.bName)
                val nameCell = createExcelCell(row, 1, rent.hName)
                createExcelCell(row, 2, rent.tName)
                createExcelCell(row, 3, rent.paidOn)
                createExcelCell(row, 4, rent.delay)
                addCommentToExcelCell(sheet, nameCell, rent.notes)
            }
        }
        // repeat(2) { sheet.createRow(rowIndex++) }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
    }
}
