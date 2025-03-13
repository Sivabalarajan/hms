package com.android.hms.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Houses
import com.android.hms.model.Rent
import com.android.hms.ui.HouseActions

class HouseRentSummaryReportAdapter(private val houseData: Map<Pair<String, String>, Map<String, Pair<Double, List<Rent>>>>) : RecyclerView.Adapter<HouseRentSummaryReportAdapter.HouseViewHolder>() {

    class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val houseTextView: TextView = itemView.findViewById(R.id.houseTextView)
        val tenantTextView: TextView = itemView.findViewById(R.id.tenantTextView)
        val monthRecyclerView: RecyclerView = itemView.findViewById(R.id.monthRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rent_summary_report_house_row, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val houseInfo = houseData.keys.elementAt(position)
        val monthData = houseData[houseInfo]!!

        holder.houseTextView.text = houseInfo.second // house name
        val house = Houses.getById(houseInfo.first)
        holder.tenantTextView.text = house?.tName?.ifEmpty { HouseActions.vacantDaysText(house.vacantDays()) } ?: "Not able to find Tenant"
        holder.houseTextView.setOnClickListener { HouseActions(holder.itemView.context, house?:return@setOnClickListener).showInfo() }

        holder.monthRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.monthRecyclerView.adapter = RentMonthlyAdapter(monthData)
    }

    override fun getItemCount() = houseData.size
}