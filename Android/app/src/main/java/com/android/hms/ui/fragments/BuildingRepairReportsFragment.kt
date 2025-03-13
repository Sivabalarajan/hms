package com.android.hms.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.android.hms.ui.adapters.BuildingRepairReportsAdapter
import com.android.hms.utils.MyProgressBar

class BuildingRepairReportsFragment(private val buildingId: String, private val buildingName: String): BaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private var adapter: BuildingRepairReportsAdapter? = null
    private lateinit var repairsList: MutableMap<String, List<Repair>>

    fun getList(): MutableMap<String, List<Repair>> { return repairsList}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_building_repair_report, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val progressBar = MyProgressBar(requireContext())
        repairsList = getUpdatedList(Repairs.getAllNotClosedByBuilding(buildingId))
        if (repairsList.isEmpty()) {
            view.findViewById<TextView>(R.id.noInfoFound).visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
        else {
            view.findViewById<TextView>(R.id.noInfoFound).visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            adapter = BuildingRepairReportsAdapter(repairsList)
            recyclerView.adapter = adapter
        }
        progressBar.dismiss()
    }

    override fun filter(searchText: String) { adapter?.filter(searchText) }

    private fun getUpdatedList(repairList: ArrayList<Repair>): MutableMap<String, List<Repair>> {
        val buildingRepairs = repairList.filter { it.hName.isEmpty() }
        val buildingHouseRepairs = if (buildingRepairs.isEmpty()) mutableMapOf() else mutableMapOf("Building: $buildingName" to buildingRepairs)
        buildingHouseRepairs.putAll(repairList.filter { it.hName.isNotEmpty() }
            .groupBy { "House: ${it.hName}" }
            .toSortedMap())

        return buildingHouseRepairs

        /* return repairList.filter { it.houseName.isNotEmpty() }
            .groupBy { it.houseName }
            .toSortedMap()
            .toMutableMap()
            .apply { put("Building: $buildingName", buildingRepairs) } */
    }
}