package com.android.hms.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.model.Rents
import com.android.hms.model.Houses
import com.android.hms.model.Rent
import com.android.hms.ui.adapters.DepositNotPaidAdapter
import com.android.hms.ui.adapters.RentLatePayerAdapter
import com.android.hms.ui.adapters.RentNotPaidAdapter
import com.android.hms.ui.adapters.VacantHouseAdapter
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuildingRentalReportsFragment(private val buildingId: String, private val buildingName: String): BaseFragment() {

    private lateinit var tvDepositNotPaid: TextView
    private lateinit var tvRentNotPaid: TextView
    private lateinit var tvVacantHouses: TextView
    private lateinit var tvRentLatePayers: TextView

    private lateinit var recyclerViewDepositNotPaid: RecyclerView
    private lateinit var recyclerViewRentNotPaid: RecyclerView
    private lateinit var recyclerViewVacantHouses: RecyclerView
    private lateinit var recyclerViewRentLatePayers: RecyclerView

    private var depositNotPaidAdapter: DepositNotPaidAdapter? = null
    private var rentNotPaidAdapter: RentNotPaidAdapter? = null
    private var vacantHouseAdapter: VacantHouseAdapter? = null
    private var rentLatePayerAdapter: RentLatePayerAdapter? = null

    private var depositNotPaidList = ArrayList<House>()
    private var rentNotPaidList = ArrayList<House>()
    private var vacantHousesList = ArrayList<House>()
    private var rentLatePayersList = ArrayList<Rent>()

    fun depositNotPaidList(): ArrayList<House> { return depositNotPaidList }
    fun rentNotPaidList(): ArrayList<House> { return rentNotPaidList }
    fun vacantHousesList(): ArrayList<House> { return vacantHousesList }
    fun rentLatePayersList(): ArrayList<Rent> { return rentLatePayersList }

    private fun initLists() {
        val progressBar = MyProgressBar(requireContext())
        lifecycleScope.launch {
            depositNotPaidList = Houses.getDepositNotPaidTenantsByBuilding(buildingId)
            rentNotPaidList = Houses.getRentNotPaidTenantsByBuilding(buildingId) // .map { RentNotPaid(it.tenantName, it.name, it.rentPendingDays()) }
            vacantHousesList = Houses.getVacantHousesByBuilding(buildingId) // .map { VacantHouseAdapter.VacantHouse(it.name, it.vacantDays()) }
            rentLatePayersList = ArrayList(withContext(Dispatchers.IO) { Rents.getRentLatePayersByBuilding(buildingId) }) // .map { RentLatePayerAdapter.RentLatePayer(it.first, it.second, it.third) }

            view?.findViewById<LinearLayout>(R.id.parentLinearLayout)?.let { parentLayout ->
                parentLayout.removeAllViews() // Remove all views from parent layout

                // Handle empty lists first
                if (depositNotPaidList.isEmpty()) {
                    tvDepositNotPaid.text = "No security deposit not paid details available. All security deposits are paid in this building $buildingName."
                    recyclerViewDepositNotPaid.visibility = View.GONE
                    parentLayout.addView(tvDepositNotPaid)
                    parentLayout.addView(recyclerViewDepositNotPaid)
                }
                if (rentNotPaidList.isEmpty()) {
                    tvRentNotPaid.text = "No rent not paid details available i.e, No rents payment pending in this building $buildingName."
                    recyclerViewRentNotPaid.visibility = View.GONE
                    parentLayout.addView(tvRentNotPaid)
                    parentLayout.addView(recyclerViewRentNotPaid)
                }
                if (vacantHousesList.isEmpty()) {
                    tvVacantHouses.text = "No vacant houses available. All houses are occupied in this building $buildingName."
                    recyclerViewVacantHouses.visibility = View.GONE
                    parentLayout.addView(tvVacantHouses)
                    parentLayout.addView(recyclerViewVacantHouses)
                }
                if (rentLatePayersList.isEmpty()) {
                    tvRentLatePayers.text = "No rent late payers details available. All rents had been paid on time in this building $buildingName so far."
                    recyclerViewRentLatePayers.visibility = View.GONE
                    parentLayout.addView(tvRentLatePayers)
                    parentLayout.addView(recyclerViewRentLatePayers)
                }

                // Handle non-empty lists
                if (depositNotPaidList.isNotEmpty()) {
                    depositNotPaidAdapter = DepositNotPaidAdapter(depositNotPaidList, false)
                    recyclerViewDepositNotPaid.adapter = depositNotPaidAdapter
                    recyclerViewDepositNotPaid.visibility = View.VISIBLE
                    parentLayout.addView(tvDepositNotPaid)
                    parentLayout.addView(recyclerViewDepositNotPaid)
                }
                if (rentNotPaidList.isNotEmpty()) {
                    rentNotPaidAdapter = RentNotPaidAdapter(rentNotPaidList, false)
                    recyclerViewRentNotPaid.adapter = rentNotPaidAdapter
                    recyclerViewRentNotPaid.visibility = View.VISIBLE
                    parentLayout.addView(tvRentNotPaid)
                    parentLayout.addView(recyclerViewRentNotPaid)
                }
                if (vacantHousesList.isNotEmpty()) {
                    vacantHouseAdapter = VacantHouseAdapter(vacantHousesList, false)
                    recyclerViewVacantHouses.adapter = vacantHouseAdapter
                    recyclerViewVacantHouses.visibility = View.VISIBLE
                    parentLayout.addView(tvVacantHouses)
                    parentLayout.addView(recyclerViewVacantHouses)
                }
                if (rentLatePayersList.isNotEmpty()) {
                    rentLatePayerAdapter = RentLatePayerAdapter(rentLatePayersList, false)
                    recyclerViewRentLatePayers.adapter = rentLatePayerAdapter
                    recyclerViewRentLatePayers.visibility = View.VISIBLE
                    parentLayout.addView(tvRentLatePayers)
                    parentLayout.addView(recyclerViewRentLatePayers)
                }
            }
            progressBar.dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_building_rent_report, container, false)

        tvDepositNotPaid = view.findViewById(R.id.tvDepositNotPaid)
        tvRentNotPaid = view.findViewById(R.id.tvRentNotPaid)
        tvVacantHouses = view.findViewById(R.id.tvVacantHouses)
        tvRentLatePayers = view.findViewById(R.id.tvRentLatePayers)

        recyclerViewDepositNotPaid = view.findViewById(R.id.recyclerViewDepositNotPaid)
        recyclerViewRentNotPaid = view.findViewById(R.id.recyclerViewRentNotPaid)
        recyclerViewVacantHouses = view.findViewById(R.id.recyclerViewVacantHouses)
        recyclerViewRentLatePayers = view.findViewById(R.id.recyclerViewRentLatePayers)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewDepositNotPaid.layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        recyclerViewDepositNotPaid.isNestedScrollingEnabled = false

        recyclerViewRentNotPaid.layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        recyclerViewRentNotPaid.isNestedScrollingEnabled = false

        recyclerViewVacantHouses.layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        recyclerViewVacantHouses.isNestedScrollingEnabled = false

        recyclerViewRentLatePayers.layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        recyclerViewRentLatePayers.isNestedScrollingEnabled = false

        initLists()
    }

    override fun filter(searchText: String) {
        depositNotPaidAdapter?.filter(searchText)
        rentNotPaidAdapter?.filter(searchText)
        vacantHouseAdapter?.filter(searchText)
        rentLatePayerAdapter?.filter(searchText)
    }
}