package com.android.hms.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Building
import com.android.hms.model.BuildingHouse
import com.android.hms.model.Buildings
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.ui.BuildingActions
import com.android.hms.ui.HouseActions
import com.android.hms.ui.activities.BaseActivity
import com.android.hms.ui.adapters.BuildingsHousesAdapter
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class BuildingsHousesFragment: BaseFragment() {

    private var buildingHouses = ArrayList<BuildingHouse>()
    private lateinit var expandableListView:ExpandableListView
    private var buildingsHousesAdapter: BuildingsHousesAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as? BaseActivity)?.setActionBarView("Buildings and Houses")
        return inflater.inflate(R.layout.fragment_buildings_houses_list_elv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = MyProgressBar(context ?: return)
        lifecycleScope.launch {
            val buildings = Buildings.getAll()
            if (buildings.isEmpty()) {
                CommonUtils.toastMessage(context ?: return@launch, "No buildings available. Please add a one now.")
                LaunchUtils.showBuildingActivity(context ?: return@launch)
                return@launch
            }
            buildingHouses = Houses.groupHousesByBuilding()

            buildings.forEach { building ->  // find buildings doesn't exist in buildingHouses from buildings and add then in buildingHouses
                if (buildingHouses.indexOfFirst { it.id == building.id } == -1) {
                    buildingHouses.add(BuildingHouse(building.id, building.name, ArrayList()))
                }
            }

            buildingHouses.sortBy { it.name }

            expandableListView = view.findViewById(R.id.expandableListView)
            buildingsHousesAdapter = BuildingsHousesAdapter(
                requireContext(),
                buildingHouses,
                onBuildingAction = { building, actionType -> buildingAction(building, actionType) },
                onHouseAction = { house, actionType -> houseAction(house, actionType) },
            )
            expandableListView.setAdapter(buildingsHousesAdapter)
            for (i in 0 until buildingHouses.size) { expandableListView.expandGroup(i) }
            setObservers()
            // (activity as? BaseActivity)?.initSearchView(view.findViewById(R.id.searchView), ::filter)
            progressBar.dismiss()
        }
        view.findViewById<FloatingActionButton>(R.id.fabAddBuilding).setOnClickListener {
            LaunchUtils.showBuildingActivity(context ?: return@setOnClickListener)
        }
    }

    override fun filter(searchText: String) {
        buildingsHousesAdapter?.filter(searchText)
    }

    private fun setObservers() {
        SharedViewModelSingleton.buildingAddedEvent.observe(viewLifecycleOwner) { building -> updateAdapterForBuildingAddition(building) }
        SharedViewModelSingleton.buildingUpdatedEvent.observe(viewLifecycleOwner) { building -> updateAdapterForBuildingChange(building) }
        SharedViewModelSingleton.buildingRemovedEvent.observe(viewLifecycleOwner) { building -> updateAdapterForBuildingRemove(building) }
        SharedViewModelSingleton.houseAddedEvent.observe(viewLifecycleOwner) { house -> updateAdapterForHouseAddition(house) }
        SharedViewModelSingleton.houseUpdatedEvent.observe(viewLifecycleOwner) { house -> updateAdapterForHouseChange(house) }
        SharedViewModelSingleton.houseRemovedEvent.observe(viewLifecycleOwner) { house -> updateAdapterForHouseRemove(house) }
    }

    private fun buildingAction(building: BuildingHouse, actionType: BuildingsHousesAdapter.BuildingActionType) {
        val progressBar = MyProgressBar(context ?: return)
        when (actionType) {
            BuildingsHousesAdapter.BuildingActionType.SELECT -> { BuildingActions(context ?: return, building.id, building.name).select() }
            BuildingsHousesAdapter.BuildingActionType.INITIATE_MAINTENANCE -> { BuildingActions(context ?: return, building.id, building.name).initiateRepair() }
            BuildingsHousesAdapter.BuildingActionType.ADD_HOUSE -> { LaunchUtils.showHouseActivity(context ?: return, building.id, building.name) }
            BuildingsHousesAdapter.BuildingActionType.EDIT_BUILDING -> { LaunchUtils.showBuildingActivity(context ?: return, building.id) }
            // else -> CommonUtils.showMessage(context ?: return, "Unknown option", "Unknown option is selected. Please try again later or try some other options.")
        }
        progressBar.dismiss()
    }

    private fun houseAction(house: House, actionType: BuildingsHousesAdapter.HouseActionType) {
        val progressBar = MyProgressBar(context ?: return)
        when (actionType) {
            BuildingsHousesAdapter.HouseActionType.SELECT -> { HouseActions(context ?: return, house).select() }
            BuildingsHousesAdapter.HouseActionType.INFO -> { HouseActions(context ?: return, house).showInfo() }
            BuildingsHousesAdapter.HouseActionType.INITIATE_MAINTENANCE -> { HouseActions(context ?: return, house).initiateRepair() }
            BuildingsHousesAdapter.HouseActionType.PAY_RENT -> { HouseActions(context ?: return, house).getRentPaid() }
            BuildingsHousesAdapter.HouseActionType.TENANT -> { if (house.tJoined > 0) makeHouseVacant(house) else makeHouseRented(house) }
            BuildingsHousesAdapter.HouseActionType.EDIT_HOUSE -> { LaunchUtils.showHouseActivity(context ?: return, house) }
            // else -> CommonUtils.showMessage(context ?: return, "Unknown option", "Unknown option is selected. Please try again later or try some other options.")
        }
        progressBar.dismiss()
    }

    private fun makeHouseRented(house: House) {
        HouseActions(context ?: return, house).assignTenant()
    }

    private fun makeHouseVacant(house: House) {
        HouseActions(context ?: return, house).makeHouseVacant()
    }

    private fun updateAdapterForHouseAddition(house: House) {
        val index = buildingHouses.indexOfFirst { it.id == house.bId }
        if (index == -1) return
        val buildingHouse = buildingHouses[index]
        if (buildingHouse.houses.indexOfFirst { it.id == house.id } != -1) return
        buildingHouse.houses.add(house)
        refreshExpandableListView(index)
        // buildingExpandableAdapter.notifyDataSetChanged()
        // buildingExpandableAdapter.notifyItemInserted(houseList.size - 1)
    }

    private fun updateAdapterForHouseChange(house: House) {
        val index = buildingHouses.indexOfFirst { it.id == house.bId }
        if (index == -1) return
        val houses = buildingHouses[index].houses
        val indexH = houses.indexOfFirst { it.id == house.id }
        if (indexH == -1) return
        houses[indexH] = house
        refreshExpandableListView(index)
        // buildingExpandableAdapter.notifyDataSetChanged()
        // buildingExpandableAdapter.notifyItemChanged(index)
    }

    private fun updateAdapterForHouseRemove(house: House) {
        val index = buildingHouses.indexOfFirst { it.id == house.bId }
        if (index == -1) return
        buildingHouses[index].houses.removeIf { it.id == house.id }
        refreshExpandableListView(index)
        // buildingExpandableAdapter.notifyDataSetChanged()
        // buildingExpandableAdapter.notifyItemRemoved(houseList.indexOf(house) + 1)
    }

    private fun updateAdapterForBuildingAddition(building: Building) {
        if (buildingHouses.indexOfFirst { it.id == building.id } != -1) return
        buildingHouses.add(BuildingHouse(building.id, building.name, ArrayList()))
        expandableListView.invalidateViews()
        // buildingExpandableAdapter.notifyDataSetChanged()
        // buildingExpandableAdapter.notifyItemInserted(buildingHouses.size - 1)
    }

    private fun updateAdapterForBuildingChange(building: Building) {
        val index = buildingHouses.indexOfFirst { it.id == building.id }
        if (index == -1) return
        if (buildingHouses[index].name == building.name) return
        buildingHouses[index].name = building.name // because only name is displayed and id will not be changed
        refreshExpandableListView(index)
        // buildingExpandableAdapter.notifyDataSetChanged()
        // buildingExpandableAdapter.notifyItemChanged(index)
    }

    private fun updateAdapterForBuildingRemove(building: Building) {
        val index = buildingHouses.indexOfFirst { it.id == building.id }
        if (index == -1) return
        buildingHouses.removeAt(index)
        refreshExpandableListView(index)
        // buildingExpandableAdapter.notifyDataSetChanged()
        // buildingExpandableAdapter.notifyItemRemoved(buildingList.indexOf(building) + 1)
    }

    private fun refreshExpandableListView(buildingPosition: Int) {
        expandableListView.collapseGroup(buildingPosition) // Collapse the group
        expandableListView.post { expandableListView.expandGroup(buildingPosition, true) } // Re-expand the group
        // expandableListView.invalidateViews()
    }

}
