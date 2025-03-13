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
import com.android.hms.utils.CommonUtils

class RentNotPaidAdapter(private var rentNotPaidList: List<House>, private val showBuildingName: Boolean = true) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val originalList = rentNotPaidList
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemCount(): Int = rentNotPaidList.size + 1 // Add 1 for header

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rent_not_paid_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rent_not_paid, parent, false)
            RentNotPaidViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RentNotPaidViewHolder) {
            val item = rentNotPaidList[position - 1] // Adjust for header
            holder.bind(item)
        } else if (holder is HeaderViewHolder) {
            holder.bind()
        }
    }

    fun filter(query: String) {
        rentNotPaidList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.tName.contains(query, ignoreCase = true) ||
                        it.bName.contains(query, ignoreCase = true)
                // it.paidOn.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTenantHeader: TextView = itemView.findViewById(R.id.tvTenantHeader)
        private val tvBuildingHeader: TextView = itemView.findViewById(R.id.tvBuildingHeader)
        private val tvHouseHeader: TextView = itemView.findViewById(R.id.tvHouseHeader)
        private val tvDaysPendingHeader: TextView = itemView.findViewById(R.id.tvDaysPendingHeader)

        fun bind() {
            tvTenantHeader.setOnClickListener { sortBy { it.tName as Comparable<Any?> } }
            if (showBuildingName) tvBuildingHeader.setOnClickListener { sortBy { it.bName as Comparable<Any?> } } else tvBuildingHeader.visibility = View.GONE
            tvHouseHeader.setOnClickListener { sortBy { it.name as Comparable<Any?> } }
            tvDaysPendingHeader.setOnClickListener { sortBy { it.rentPendingDays() as Comparable<Any?> } }
        }

        private fun sortBy(selector: (House) -> Comparable<Any?>) {
            rentNotPaidList = rentNotPaidList.sortedBy(selector)
            notifyDataSetChanged()
        }
    }

    inner class RentNotPaidViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tenantName: TextView = view.findViewById(R.id.tvTenant)
        private val buildingName: TextView = view.findViewById(R.id.tvBuilding)
        private val houseName: TextView = view.findViewById(R.id.tvHouse)
        private val daysPending: TextView = view.findViewById(R.id.tvDaysPending)
        private val btnPayRent: ImageButton = view.findViewById(R.id.btnPayRent)
        private val btnRemoveTenant: ImageButton = view.findViewById(R.id.btnRemoveTenant)
        fun bind(house: House) {
            tenantName.text = house.tName
            if (showBuildingName) buildingName.text = house.bName else buildingName.visibility = View.GONE
            houseName.text = house.name
            daysPending.text = CommonUtils.formatNumToText(house.rentPendingDays()) // HouseActions.rentPendingDaysText(house.rentPendingDays())
            btnPayRent.setOnClickListener { HouseActions(btnPayRent.context, house).getRentPaid() }
            btnRemoveTenant.setOnClickListener { HouseActions(btnRemoveTenant.context, house).makeHouseVacant() }
        }
    }
}