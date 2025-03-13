package com.android.hms.ui.activities

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.hms.databinding.ActivityCommonListBinding
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.ui.HouseActions
import com.android.hms.ui.adapters.VacantHouseAdapter
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class VacantHousesReportActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityCommonListBinding
    private var adapter: VacantHouseAdapter? = null
    private var vacantHousesList = ArrayList<House>()

    override fun getReportName(): String { return "Vacant Houses" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommonListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.description.text = "View all vacant houses"

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val progressBar = MyProgressBar(this)
        lifecycleScope.launch {
            vacantHousesList = Houses.getVacantHouses()
            if (vacantHousesList.isEmpty()) CommonUtils.showMessage(context, "No vacant houses","No vacant houses available. All houses are occupied.")
            else {
                adapter = VacantHouseAdapter(vacantHousesList)
                binding.recyclerView.adapter = adapter
                // initSearchView(binding.searchView)
            }
            progressBar.dismiss()
        }
        setActionBarView(getReportName())
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    override fun saveToOutputStream(outputStream: OutputStream) {
        if (vacantHousesList.isEmpty()) {
            launch { CommonUtils.showMessage(context, "No vacant houses","No vacant houses available. All houses are occupied.") }
            return
        }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0
        var row = sheet.createRow(rowIndex++)
        if (vacantHousesList.isEmpty()) createHeaderExcelCell(row, 0, "No vacant houses available. All houses are occupied.")
        else {
            createHeaderExcelCell(row, 0, getReportName())
            row = sheet.createRow(rowIndex++)
            createHeaderExcelCell(row, 0, "Building")
            createHeaderExcelCell(row, 1, "House")
            createHeaderExcelCell(row, 2, "Vacant in Days")
            createHeaderExcelCell(row, 3, "Vacant info")
            for (house in vacantHousesList) {
                row = sheet.createRow(rowIndex++)
                createExcelCell(row, 0, house.bName)
                val nameCell = createExcelCell(row, 1, house.name)
                val vacantDays = house.vacantDays()
                createExcelCell(row, 2, vacantDays)
                createExcelCell(row, 3, HouseActions.vacantDaysText(vacantDays))
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