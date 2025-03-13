package com.android.hms.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Repair
import com.android.hms.ui.RepairActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils

class RepairReportAdapter(private var repairsList: MutableList<Repair>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val originalList = repairsList
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemCount(): Int = repairsList.size + 1 // Add 1 for header

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.report_repair_item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.report_repair_item_row, parent, false)
            RepairReportViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RepairReportViewHolder) {
            val repair = repairsList[position - 1] // Adjust for header
            holder.bind(repair)
        } else if (holder is HeaderViewHolder) {
            holder.bind()
        }
    }

    fun filter(query: String) {
        repairsList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.desc.contains(query, ignoreCase = true) ||
                        "${it.amount}".contains(query, ignoreCase = true) ||
                        it.status.contains(query, ignoreCase = true)
//                        it.tName.contains(query, ignoreCase = true) ||
//                        it.bName.contains(query, ignoreCase = true)
                // it.paidOn.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun updateForAddition(repair: Repair) {
        if (repairsList.indexOfFirst { it.id == repair.id } != -1) return
        repairsList.add(repair)
        notifyItemInserted(repairsList.size) // header row is included
    }

    fun updateForChange(repair: Repair) {
        val index = repairsList.indexOfFirst { it.id == repair.id }
        if (index == -1) updateForAddition(repair)
        else {
            repairsList[index] = repair
            notifyItemChanged(index + 1) // header row is included
        }
    }

    fun updateForRemove(repair: Repair) {
        val index = repairsList.indexOfFirst { it.id == repair.id }
        if (index == -1) return
        repairsList.removeAt(index)
        notifyItemRemoved(index + 1) // header row is included
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRepairHeaderDescription: TextView = itemView.findViewById(R.id.tvRepairHeaderDescription)
        private val tvRepairHeaderBuildingHouse: TextView = itemView.findViewById(R.id.tvRepairHeaderBuildingHouse)
        private val tvRepairHeaderAmount: TextView = itemView.findViewById(R.id.tvRepairHeaderAmount)
        private val tvRepairHeaderRaisedDate: TextView = itemView.findViewById(R.id.tvRepairHeaderRaisedDate)
        private val tvRepairHeaderStatus: TextView = itemView.findViewById(R.id.tvRepairHeaderStatus)

        fun bind() {
            tvRepairHeaderDescription.setOnClickListener { sortRepairsBy { it.desc as Comparable<Any?> } }
            tvRepairHeaderBuildingHouse.setOnClickListener { sortRepairsBy { it.bName as Comparable<Any?> } }
            tvRepairHeaderAmount.setOnClickListener { sortRepairsBy { it.amount as Comparable<Any?> } }
            tvRepairHeaderRaisedDate.setOnClickListener { sortRepairsBy { it.raisedOn as Comparable<Any?> } }
            tvRepairHeaderStatus.setOnClickListener { sortRepairsBy { it.status as Comparable<Any?> } }
        }

        private fun sortRepairsBy(selector: (Repair) -> Comparable<Any?>) {
            repairsList = repairsList.sortedBy(selector).toMutableList()
            notifyDataSetChanged()
        }
    }

    class RepairReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRepairDescription: TextView = itemView.findViewById(R.id.tvRepairDescription)
        private val tvRepairBuildingHouse: TextView = itemView.findViewById(R.id.tvRepairBuildingHouse)
        private val tvRepairAmount: TextView = itemView.findViewById(R.id.tvRepairAmount)
        private val tvRepairRaisedDate: TextView = itemView.findViewById(R.id.tvRepairRaisedDate)
        private val tvRepairStatus: TextView = itemView.findViewById(R.id.tvRepairStatus)
        private val btnRepairInfo: ImageButton = itemView.findViewById(R.id.btnRepairInfo)
        private val btnCloseRepair: ImageButton = itemView.findViewById(R.id.btnCloseRepair)
        private val btnRemoveRepair: ImageButton = itemView.findViewById(R.id.btnRemoveRepair)

        fun bind(repair: Repair) {
            tvRepairDescription.text = repair.desc
            tvRepairBuildingHouse.text = repair.hName.ifEmpty { repair.bName }
            tvRepairAmount.text = CommonUtils.formatNumToText(repair.amount)
            tvRepairRaisedDate.text = CommonUtils.getShortDayMonthFormatText(repair.raisedOn)
            tvRepairStatus.text = repair.status
            tvRepairDescription.setOnClickListener { LaunchUtils.showRepairActivity(tvRepairDescription.context, repair) }
            itemView.setOnClickListener { LaunchUtils.showRepairActivity(tvRepairDescription.context, repair) }
            btnRepairInfo.setOnClickListener { RepairActions(btnRepairInfo.context, repair).showInfo() }
            btnCloseRepair.setOnClickListener { RepairActions(btnCloseRepair.context, repair).closeRepair(repair.notes) }
            btnRemoveRepair.setOnClickListener { RepairActions(btnRemoveRepair.context, repair).removeRepair() }
        }
    }
}