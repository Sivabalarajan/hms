package com.android.hms.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.ui.HouseActions

class VacantHouseAdapter(private var vacantHousesList: List<House>, private val showBuildingName: Boolean = true) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val originalList = vacantHousesList
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemCount(): Int = vacantHousesList.size + 1 // Add 1 for header

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vacant_house_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vacant_house, parent, false)
            VacantHouseViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is VacantHouseViewHolder) {
            val item = vacantHousesList[position - 1] // Adjust for header
            holder.bind(item)
        } else if (holder is HeaderViewHolder) {
            holder.bind()
        }
    }

    fun filter(query: String) {
        vacantHousesList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.bName.contains(query, ignoreCase = true)
                // it.paidOn.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBuildingHeader: TextView = itemView.findViewById(R.id.tvBuildingHeader)
        private val tvHouseHeader: TextView = itemView.findViewById(R.id.tvHouseHeader)
        private val tvVacantDaysHeader: TextView = itemView.findViewById(R.id.tvVacantDaysHeader)

        fun bind() {
            if (showBuildingName) tvBuildingHeader.setOnClickListener { sortBy { it.bName as Comparable<Any?> } } else tvBuildingHeader.visibility = View.GONE
            tvHouseHeader.setOnClickListener { sortBy { it.name as Comparable<Any?> } }
            tvVacantDaysHeader.setOnClickListener { sortBy { it.vacantDays() as Comparable<Any?> } }
        }

        private fun sortBy(selector: (House) -> Comparable<Any?>) {
            vacantHousesList = vacantHousesList.sortedBy(selector)
            notifyDataSetChanged()
        }
    }

    inner class VacantHouseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val buildingName: TextView = view.findViewById(R.id.tvBuilding)
        private val houseName: TextView = view.findViewById(R.id.tvHouse)
        private val vacantDays: TextView = view.findViewById(R.id.tvVacantDays)
        private val btnAssignTenant: ImageButton = view.findViewById(R.id.btnAssignTenant)
        fun bind(house: House) {
            if (showBuildingName) buildingName.text = house.bName else buildingName.visibility = View.GONE
            houseName.text = house.name
            vacantDays.text = HouseActions.vacantDaysText(house.vacantDays())
            btnAssignTenant.setOnClickListener { HouseActions(btnAssignTenant.context, house).assignTenant() }
        }
    }
}