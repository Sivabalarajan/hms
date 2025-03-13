package com.android.hms.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.android.hms.R
import com.android.hms.model.Building
import com.android.hms.model.Buildings
import com.android.hms.ui.activities.BaseActivity
import com.android.hms.ui.adapters.BuildingAdapter
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class BuildingsFragment: BaseFragment() {

    private var buildingList = ArrayList<Building>()
    private lateinit var recyclerView: RecyclerView
    private var buildingAdapter: BuildingAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_buildings_list, container, false)
        val context = context ?: return view
        val progressBar = MyProgressBar(context)

        (activity as? BaseActivity)?.setActionBarView("Buildings")

        recyclerView = view.findViewById(R.id.recycler_view_buildings)
        recyclerView.layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL) // GridLayoutManager(context, 3)
        // recyclerView.layoutManager = LinearLayoutManager(context) // GridLayoutManager(context, columnCount)

        lifecycleScope.launch {
            buildingList = ArrayList(Buildings.getAll().sortedBy { it.name })
            if (buildingList.isEmpty()) {
                CommonUtils.toastMessage(context, "No buildings available. Please add a one now.")
                LaunchUtils.showBuildingActivity(context)
            }
            buildingAdapter = BuildingAdapter(buildingList)
            recyclerView.adapter = buildingAdapter
            setObservers()
            // (activity as? BaseActivity)?.initSearchView(view.findViewById(R.id.searchView), ::filter)
            progressBar.dismiss()
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_building).setOnClickListener {
            LaunchUtils.showBuildingActivity(context)
        }

        return view
    }

    override fun filter(searchText: String) {
        buildingAdapter?.filter(searchText)
    }

    private fun setObservers() {
        SharedViewModelSingleton.buildingAddedEvent.observe(viewLifecycleOwner) { building -> updateAdapterForBuildingAddition(building) }
        SharedViewModelSingleton.buildingUpdatedEvent.observe(viewLifecycleOwner) { building -> updateAdapterForBuildingChange(building) }
        SharedViewModelSingleton.buildingRemovedEvent.observe(viewLifecycleOwner) { building -> updateAdapterForBuildingRemove(building) }

        SharedViewModelSingleton.houseAddedEvent.observe(viewLifecycleOwner) { house -> updateAdapterForHouseChange(house.bId) }
        SharedViewModelSingleton.houseRemovedEvent.observe(viewLifecycleOwner) { house -> updateAdapterForHouseChange(house.bId) }
    }

    private fun updateAdapterForBuildingAddition(building: Building) {
        if (buildingList.indexOfFirst { it.id == building.id } != -1) return
        buildingList.add(building)
        buildingAdapter?.notifyItemInserted(buildingList.size - 1)
    }

    private fun updateAdapterForHouseChange(buildingId: String) {
        val index = buildingList.indexOfFirst { it.id == buildingId }
        if (index == -1) return
        buildingAdapter?.notifyItemChanged(index)
    }

    private fun updateAdapterForBuildingChange(building: Building) {
        val index = buildingList.indexOfFirst { it.id == building.id }
        if (index == -1) {
            updateAdapterForBuildingAddition(building)
            return
        }
        buildingList[index] = building
        buildingAdapter?.notifyItemChanged(index)
    }

    private fun updateAdapterForBuildingRemove(building: Building) {
        val index = buildingList.indexOfFirst { it.id == building.id }
        if (index == -1) return
        buildingList.removeAt(index)
        buildingAdapter?.notifyItemRemoved(index)
    }
}