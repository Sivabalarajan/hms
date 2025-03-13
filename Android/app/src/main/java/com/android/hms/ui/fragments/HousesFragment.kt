package com.android.hms.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.ui.activities.BaseActivity
import com.android.hms.ui.adapters.HouseAdapter
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class HousesFragment(private var buildingId: String = "", private var buildingName: String = ""): BaseFragment() {

    private var houseList = ArrayList<House>()
    private lateinit var recyclerView: RecyclerView
    private var houseAdapter: HouseAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (buildingId.isEmpty()) {
            buildingId = arguments?.getString("buildingId") ?: ""
            buildingName = arguments?.getString("buildingName") ?: ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_houses_list, container, false)
        val progressBar = MyProgressBar(context ?: return view)

        val titleText = if (buildingName.isEmpty()) "All Houses" else "Houses in $buildingName"
        (activity as? BaseActivity)?.setActionBarView(titleText)

        if (buildingId.isEmpty()) view.findViewById<FloatingActionButton>(R.id.fab_add_house).visibility = View.GONE
        else {
            val fabAddHouse = view.findViewById<FloatingActionButton>(R.id.fab_add_house)
            fabAddHouse.visibility = View.VISIBLE
            fabAddHouse.setOnClickListener {
                LaunchUtils.showHouseActivity(context ?: return@setOnClickListener, buildingId, buildingName)
            }
        }
        // view.findViewById<TextView>(R.id.tvBuildingName).text = titleText

        recyclerView = view.findViewById(R.id.recycler_view_houses)
        // recyclerView.layoutManager = LinearLayoutManager(context)    // when {          columnCount <= 1 -> LinearLayoutManager(context)             else -> GridLayoutManager(context, columnCount)         }
        recyclerView.layoutManager = GridLayoutManager(context, 4) // StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)

        lifecycleScope.launch {
            houseList = ArrayList((if (buildingId.isEmpty()) Houses.getAll() else Houses.getAllByBuilding(buildingId)).sortedBy { it.name })
            if (houseList.isEmpty()) {
                if (buildingId.isNotEmpty()) {
                    CommonUtils.toastMessage(context ?: return@launch, "No houses available. Please add a one now.")
                    LaunchUtils.showHouseActivity(context ?: return@launch, buildingId, buildingName)
                }
                else CommonUtils.showMessage(context ?: return@launch, "No houses available", "Please select a building to add a house.")
            }
            houseAdapter = HouseAdapter(houseList)
            recyclerView.adapter = houseAdapter
            setObservers()
            // (activity as? BaseActivity)?.initSearchView(view.findViewById(R.id.searchView), ::filter)
            progressBar.dismiss()
        }

        return view
    }

    override fun filter(searchText: String) {
        houseAdapter?.filter(searchText)
    }

    private fun setObservers() {
        SharedViewModelSingleton.houseAddedEvent.observe(viewLifecycleOwner) { house -> updateAdapterForAddition(house) }
        SharedViewModelSingleton.houseUpdatedEvent.observe(viewLifecycleOwner) { house -> updateAdapterForChange(house) }
        SharedViewModelSingleton.houseRemovedEvent.observe(viewLifecycleOwner) { house -> updateAdapterForRemove(house) }
    }

    private fun updateAdapterForAddition(house: House) {
        if (houseList.indexOfFirst { it.id == house.id } != -1) return
        houseList.add(house)
        houseAdapter?.notifyItemInserted(houseList.size - 1)
    }

    private fun updateAdapterForChange(house: House) {
        val index = houseList.indexOfFirst { it.id == house.id }
        if (index == -1) {
            updateAdapterForAddition(house)
            return
        }
        houseList[index] = house
        houseAdapter?.notifyItemChanged(index)
    }

    private fun updateAdapterForRemove(house: House) {
        val index = houseList.indexOfFirst { it.id == house.id }
        if (index == -1) return
        houseList.removeAt(index)
        houseAdapter?.notifyItemRemoved(index)
    }
}