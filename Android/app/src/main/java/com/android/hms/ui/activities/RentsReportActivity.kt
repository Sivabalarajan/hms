package com.android.hms.ui.activities

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.hms.databinding.ActivityCommonListBinding
import com.android.hms.model.Rent
import com.android.hms.model.Rents
import com.android.hms.ui.adapters.RentReportAdapter
import com.android.hms.ui.reports.ReportsBaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

class RentsReportActivity: ReportsBaseActivity() {

    private lateinit var binding: ActivityCommonListBinding
    private var adapter: RentReportAdapter? = null
    private var rentsList: MutableList<Rent> = mutableListOf()

    override fun getReportName(): String { return "All Rent Details" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommonListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.description.visibility = View.GONE
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val progressBar = MyProgressBar(this, "Please wait... Loading rent details...")
        lifecycleScope.launch {
            rentsList = withContext(Dispatchers.IO) { Rents.getAllFromDb().sortedBy { it.paidOn }.toMutableList() }
            if (rentsList.isEmpty()) CommonUtils.showMessage(context, "No rents", "No rent details found at this point of time.")
            else {
                adapter = RentReportAdapter(rentsList)
                binding.recyclerView.adapter = adapter
                // initSearchView(binding.searchView)
            }
            initializeObservers()
            progressBar.dismiss()
        }
        setActionBarView(getReportName())
    }

    private fun initializeObservers() {
        SharedViewModelSingleton.rentPaidEvent.observe(this) { rent -> updateAdapterForAddition(rent) }
        SharedViewModelSingleton.rentUpdatedEvent.observe(this) { rent -> updateAdapterForChange(rent) }
        SharedViewModelSingleton.rentRemovedEvent.observe(this) { rent -> updateAdapterForRemove(rent) }
    }

    private fun updateAdapterForAddition(rent: Rent) {
        if (rentsList.indexOfFirst { it.id == rent.id } != -1) return
        rentsList.add(rent)
        adapter?.notifyItemInserted(rentsList.size) // header row is included
    }

    private fun updateAdapterForChange(rent: Rent) {
        val index = rentsList.indexOfFirst { it.id == rent.id }
        if (index == -1) {
            updateAdapterForAddition(rent)
            return
        }
        rentsList[index] = rent
        adapter?.notifyItemChanged(index + 1) // header row is included
    }

    private fun updateAdapterForRemove(rent: Rent) {
        val index = rentsList.indexOfFirst { it.id == rent.id }
        if (index == -1) return
        rentsList.removeAt(index)
        adapter?.notifyItemRemoved(index + 1) // header row is included
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    override fun saveToOutputStream(outputStream: OutputStream) {
        if (rentsList.isEmpty()) {
            launch { CommonUtils.showMessage(context, "No rents", "No rent details found at this point of time.") }
            return
        }
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(getReportName())
        var rowIndex = 0
        val headerRow = sheet.createRow(rowIndex++)
        createHeaderExcelCell(headerRow, 0, "Building")
        createHeaderExcelCell(headerRow, 1, "House")
        createHeaderExcelCell(headerRow, 2, "Tenant")
        createHeaderExcelCell(headerRow, 3, "Amount")
        createHeaderExcelCell(headerRow, 4, "Delay in Days")
        createHeaderExcelCell(headerRow, 5, "Paid On")

        var total = 0
        rentsList.forEach { rent ->
            val row = sheet.createRow(rowIndex++)
            createExcelCell(row, 0, rent.bName)
            val nameCell = createExcelCell(row, 1, rent.hName)
            createExcelCell(row, 2, rent.tName)
            createExcelCell(row, 3, rent.amount)
            createExcelCell(row, 4, rent.delay)
            createExcelCell(row, 5, rent.paidOn)

            addCommentToExcelCell(sheet, nameCell, rent.notes)
            total += rent.amount
        }
        sheet.createRow(rowIndex++)
        val totalRow = sheet.createRow(rowIndex++)
        createHeaderExcelCell(totalRow, 0, "Total")
        createHeaderExcelCell(totalRow, 3, total)

        // repeat(2) { sheet.createRow(rowIndex++) }

        autoAdjustRowsColumnsHeight(sheet)

        // Write the output to a file
        workbook.write(outputStream)
        workbook.close()
    }
}