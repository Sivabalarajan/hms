package com.android.hms.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Repairs
import com.android.hms.model.House
import com.android.hms.model.Rents
import com.android.hms.ui.HouseActions
import com.android.hms.ui.adapters.RepairReportAdapter
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TenantHouseFragment(private val house: House): BaseFragment() {

    private lateinit var repairsRecyclerView: RecyclerView
    private lateinit var houseTableLayout: TableLayout
    private var adapter: RepairReportAdapter? = null
    private var tvNoRepairs: TextView? = null

    fun getName(): String = house.name

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tenant_house, container, false)
        houseTableLayout = view.findViewById(R.id.houseTableLayout)
        repairsRecyclerView = view.findViewById(R.id.repairsRecyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val progressBar = MyProgressBar(requireContext())
        createTableRows()
        repairsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        refreshAdapter()
        addSubmitRepairRow()
        setObservers()
        progressBar.dismiss()
    }

    private fun setObservers() {
        SharedViewModelSingleton.repairInitiatedEvent.observe(viewLifecycleOwner) {
            if (it.hId == house.id) refreshAdapter()
        }
        SharedViewModelSingleton.repairUpdatedEvent.observe(viewLifecycleOwner) {
            if (it.hId == house.id) refreshAdapter()
        }
        SharedViewModelSingleton.repairRemovedEvent.observe(viewLifecycleOwner) {
            if (it.hId == house.id) refreshAdapter()
        }
    }

    override fun filter(searchText: String) {
        adapter?.filter(searchText)
    }

    private fun refreshAdapter() {
        val progressBar = MyProgressBar(requireContext())
        lifecycleScope.launch {
            val repairsList = withContext(Dispatchers.IO) { Repairs.getAllNotClosedByHouse(house.id) }
            if (repairsList.isEmpty()) {
                repairsRecyclerView.visibility = View.GONE
                addNoRepairsRow()
            } else {
                removeNoRepairsRow()
                repairsRecyclerView.visibility = View.VISIBLE
                adapter = RepairReportAdapter(repairsList)
                repairsRecyclerView.adapter = adapter
            }
            progressBar.dismiss()
        }
    }

    private fun createTableRows() {
        lifecycleScope.launch {
            val progressBar = MyProgressBar(requireContext())

            houseTableLayout.addView(createTableRow("House Name", house.name))
            houseTableLayout.addView(createTableRow("Building", house.bName))
            houseTableLayout.addView(createTableRow("Joined On", CommonUtils.getFullDayDateFormatText(house.tJoined)))
            val depositText = "${HouseActions.depositPendingDaysText(house.depositPendingDays())} and the amount is ${CommonUtils.formatNumToText(house.deposit)}"
            houseTableLayout.addView(createTableRow("Deposit", depositText))
            houseTableLayout.addView(createTableRow("Rent", CommonUtils.formatNumToText(house.rent)))
            houseTableLayout.addView(createTableRow("Revised On", CommonUtils.getFullDayDateFormatText(house.rRevised)))
            val rentPendingDays = house.rentPendingDays()
            val rentPendingDaysText = if (rentPendingDays > 0) HouseActions.rentPendingDaysText(rentPendingDays) else ""
            val textForLastRentPaid = if (house.rPaid == 0L) rentPendingDaysText
            else {
                val delayedRentPayments = CommonUtils.formatIntList(withContext(Dispatchers.IO) { (Rents.getTenantDelayedPaymentsByHouse(house.tId, house.id))})
                if (delayedRentPayments.isEmpty()) CommonUtils.getDayMonthFormatText(house.rPaid) + ". $rentPendingDaysText"
                else CommonUtils.getDayMonthFormatText(house.rPaid) + ". $rentPendingDaysText (Delayed by $delayedRentPayments days earlier)"
            }
            houseTableLayout.addView(createTableRow("Last Rent Paid", textForLastRentPaid))

            /*
            val header = TableRow(context)
            header.addView(createTextView("House Name"))
            header.addView(createTextView("Building"))
            header.addView(createTextView("Joined On"))
            header.addView(createTextView("Deposit"))
            header.addView(createTextView("Rent"))
            header.addView(createTextView("Revised On"))
            header.addView(createTextView("Last Rent Paid"))
            tvHouseTableLayout.addView(header)

            val info = TableRow(context)
            info.addView(createTextView(house.name))
            info.addView(createTextView(house.bName))
            info.addView(createTextView(CommonUtils.getFullDayDateFormatText(house.tJoined)))
            info.addView(createTextView("${HouseActions.depositPendingDaysText(house.depositPendingDays())} and the amount is ${CommonUtils.formatNumToText(house.deposit)}"))
            info.addView(createTextView(CommonUtils.formatNumToText(house.rent)))
            info.addView(createTextView(CommonUtils.getFullDayDateFormatText(house.rRevised)))
            val rentPendingDays = house.rentPendingDays()
            val rentPendingDaysText = if (rentPendingDays > 0) HouseActions.rentPendingDaysText(rentPendingDays) else ""
            if (house.rPaid == 0L) info.addView(createTextView(rentPendingDaysText))
            else {
                val delayedRentPayments = CommonUtils.formatIntList(withContext(Dispatchers.IO) { (Rents.getTenantDelayedPaymentsByHouse(house.tId, house.id))})
                if (delayedRentPayments.isEmpty()) info.addView(createTextView(CommonUtils.getDayMonthFormatText(house.rPaid) + ". $rentPendingDaysText"))
                else info.addView(createTextView(CommonUtils.getDayMonthFormatText(house.rPaid) + ". $rentPendingDaysText (Delayed by $delayedRentPayments days earlier)"))
            }
            tvHouseTableLayout.addView(info) */

            progressBar.dismiss()
        }
    }

    private fun createTableRow(columnName: String, columnValue: String): TableRow {
        val tableRow = TableRow(context)
        tableRow.addView(createTV(columnName))
        tableRow.addView(createTV(" : ")) // add some delimiter with space
        tableRow.addView(createTV(columnValue))
        return tableRow
    }

    private fun createTV(text: String): TextView {
        val textView = TextView(context)
        textView.text = text
        textView.setPadding(5 )
        // textView.setPadding(5, 5, 5, 5)
        return textView
    }

    private fun addNoRepairsRow() {
        if (tvNoRepairs != null) return // already added
        // tvHouseTableLayout.addView(TableRow(context)) // add an empty row
        tvNoRepairs = TextView(context)
        tvNoRepairs?.text = "No repairs found for this house."
        tvNoRepairs?.setPadding(10, 20, 10, 10)
        houseTableLayout.addView(tvNoRepairs)
    }

    private fun removeNoRepairsRow() {
        tvNoRepairs?.let {
            houseTableLayout.removeView(it)
            tvNoRepairs = null
        }
    }

    private fun addSubmitRepairRow() {
        houseTableLayout.addView(TableRow(context)) // add an empty row
        val btnNewRepair = Button(context)
        btnNewRepair.text = "Submit New Repair"
        btnNewRepair.setPadding(10)
        btnNewRepair.setOnClickListener {
            LaunchUtils.showRepairActivity(requireContext(), house.bId, house.bName, house.id)
        }
        houseTableLayout.addView(btnNewRepair)
    }
}