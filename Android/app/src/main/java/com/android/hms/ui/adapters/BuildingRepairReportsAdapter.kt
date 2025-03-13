package com.android.hms.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Repair
import com.android.hms.ui.adapters.BuildingRepairReportsAdapter.BuildingRepairReportViewHolder

class BuildingRepairReportsAdapter(private var repairList: Map<String, List<Repair>>) : RecyclerView.Adapter<BuildingRepairReportViewHolder>() {

    private val originalList = repairList

    private var headers = repairList.keys.toList() // Extract headers (titles)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingRepairReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.report_building_repair_item, parent, false)
        return BuildingRepairReportViewHolder(view)
    }

    override fun getItemCount(): Int  = headers.size

    override fun onBindViewHolder(holder: BuildingRepairReportViewHolder, position: Int) {
        val header = headers[position]
        holder.bind(header, repairList[header] ?: return)
    }

    fun filter(query: String) {
        repairList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.key.contains(query, ignoreCase = true) ||
                (it.value.find { repair -> repair.desc.contains(query, ignoreCase = true) } != null)
                (it.value.find { repair -> repair.status.contains(query, ignoreCase = true) } != null)
            }
        }
        notifyDataSetChanged()
    }

    class BuildingRepairReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerBuildingOrHouse: TextView = view.findViewById(R.id.headerBuildingOrHouse)
        private val childRepairReportRecyclerView: RecyclerView = view.findViewById(R.id.childRepairReportRecyclerView)
        private val expandCollapseIcon: ImageView = itemView.findViewById(R.id.expandCollapseIcon)

        private var isExpanded = true

        fun bind(header: String, repairs: List<Repair>) {
            headerBuildingOrHouse.text = header

            // Set up child RecyclerView
            childRepairReportRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            childRepairReportRecyclerView.adapter = RepairReportAdapter(repairs.toMutableList())

            expandCollapseIcon.setColorFilter(headerBuildingOrHouse.currentTextColor)

            headerBuildingOrHouse.setOnClickListener { toggleExpandCollapse() }
        }
        private fun toggleExpandCollapse() {
            isExpanded = !isExpanded
            updateIconAndVisibility()
        }

        private fun updateIconAndVisibility() {
            childRepairReportRecyclerView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandCollapseIcon.setImageResource(if (isExpanded) R.drawable.ic_expand else R.drawable.ic_collapse_right)
        }
    }
}
