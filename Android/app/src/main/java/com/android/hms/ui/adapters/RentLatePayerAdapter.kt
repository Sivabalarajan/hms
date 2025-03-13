package com.android.hms.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Rent
import com.android.hms.utils.CommonUtils

class RentLatePayerAdapter(private var rentLatePayersList: List<Rent>, private val showBuildingName: Boolean = true) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val originalList = rentLatePayersList
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemCount(): Int = rentLatePayersList.size + 1 // Add 1 for header

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rent_late_payer_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rent_late_payer, parent, false)
            RentLatePayerViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RentLatePayerViewHolder) {
            val rent = rentLatePayersList[position - 1] // Adjust for header
            holder.bind(rent)
        } else if (holder is HeaderViewHolder) {
            holder.bind()
        }
    }

    fun filter(query: String) {
        rentLatePayersList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.tName.contains(query, ignoreCase = true) ||
                        it.hName.contains(query, ignoreCase = true) ||
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
        private val tvMonthHeader: TextView = itemView.findViewById(R.id.tvMonthHeader)
        private val tvDelayDaysHeader: TextView = itemView.findViewById(R.id.tvDelayDaysHeader)

        fun bind() {
            tvTenantHeader.setOnClickListener { sortBy { it.tName as Comparable<Any?> } }
            if (showBuildingName) tvBuildingHeader.setOnClickListener { sortBy { it.bName as Comparable<Any?> } } else tvBuildingHeader.visibility = View.GONE
            tvHouseHeader.setOnClickListener { sortBy { it.hName as Comparable<Any?> } }
            tvMonthHeader.setOnClickListener { sortBy { it.paidOn as Comparable<Any?> } }
            tvDelayDaysHeader.setOnClickListener { sortBy { it.delay as Comparable<Any?> } }
        }

        private fun sortBy(selector: (Rent) -> Comparable<Any?>) {
            rentLatePayersList = rentLatePayersList.sortedBy(selector)
            notifyDataSetChanged()
        }
    }

    inner class RentLatePayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tenantName: TextView = view.findViewById(R.id.tvTenant)
        private val buildingName: TextView = view.findViewById(R.id.tvBuilding)
        private val houseName: TextView = view.findViewById(R.id.tvHouse)
        private val tvMonth: TextView = view.findViewById(R.id.tvMonth)
        private val delayDays: TextView = view.findViewById(R.id.tvDelay)
        fun bind(rent: Rent) {
            tenantName.text = rent.tName
            if (showBuildingName) buildingName.text = rent.bName else buildingName.visibility = View.GONE
            houseName.text = rent.hName
            tvMonth.text = CommonUtils.getMonthYearOnlyFormatText(rent.paidOn)
            delayDays.text = CommonUtils.formatNumToText(rent.delay)
        }
    }
}