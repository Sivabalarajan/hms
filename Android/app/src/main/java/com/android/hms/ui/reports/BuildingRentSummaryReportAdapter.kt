package com.android.hms.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Rent

class BuildingRentSummaryReportAdapter(private val reportData: Map<String, Map<Pair<String, String>, Map<String, Pair<Double, List<Rent>>>>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    class BuildingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val buildingTextView: TextView = itemView.findViewById(R.id.buildingTextView)
        val houseRecyclerView: RecyclerView = itemView.findViewById(R.id.houseRecyclerView)
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerHouseTextView: TextView = itemView.findViewById(R.id.headerHouseTextView)
        val headerTenantTextView: TextView = itemView.findViewById(R.id.headerTenantTextView)
        val headerMonthTextView: TextView = itemView.findViewById(R.id.headerMonthTextView)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.building_rent_summary_report_header_row, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_building_rent_house_report_row, parent, false)
            BuildingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            // val months = reportData.values.flatMap { it.values.flatMap { it.keys } }.distinct().sorted()
            // holder.headerMonthTextView.text = months.joinToString("\t") // all months are displayed - this is not wanted
        } else if (holder is BuildingViewHolder) {
            val buildingName = reportData.keys.elementAt(position - 1)
            val houseData = reportData[buildingName]!!

            holder.buildingTextView.text = buildingName

            holder.houseRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.houseRecyclerView.adapter = HouseRentSummaryReportAdapter(houseData)
        }
    }

    override fun getItemCount() = reportData.size + 1 // header consideration
}