package com.android.hms.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Rent
import com.android.hms.ui.RentActions
import com.android.hms.utils.CommonUtils

class RentReportAdapter(private var rentsList: MutableList<Rent>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val originalList = rentsList
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemCount(): Int = rentsList.size + 1 // Add 1 for header

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rent_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rent, parent, false)
            RentViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RentViewHolder) {
            val rent = rentsList[position - 1] // Adjust for header
            holder.bind(rent)
        } else if (holder is HeaderViewHolder) {
            holder.bind()
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTenantHeader: TextView = itemView.findViewById(R.id.tvTenantHeader)
        private val tvHouseHeader: TextView = itemView.findViewById(R.id.tvHouseHeader)
        private val tvAmountHeader: TextView = itemView.findViewById(R.id.tvAmountHeader)
        private val tvDelayDaysHeader: TextView = itemView.findViewById(R.id.tvDelayDaysHeader)
        private val tvPaidOnHeader: TextView = itemView.findViewById(R.id.tvPaidOnHeader)

        fun bind() {
            tvTenantHeader.setOnClickListener { sortBy { it.tName as Comparable<Any?> } }
            tvHouseHeader.setOnClickListener { sortBy { it.hName as Comparable<Any?> } }
            tvAmountHeader.setOnClickListener { sortBy { it.amount as Comparable<Any?> } }
            tvDelayDaysHeader.setOnClickListener { sortBy { it.delay as Comparable<Any?> } }
            tvPaidOnHeader.setOnClickListener { sortBy { it.paidOn as Comparable<Any?> } }
        }

        private fun sortBy(selector: (Rent) -> Comparable<Any?>) {
            rentsList = rentsList.sortedBy(selector).toMutableList()
            notifyDataSetChanged()
        }
    }

    class RentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTenant: TextView = view.findViewById(R.id.tvTenant)
        private val tvHouse: TextView = view.findViewById(R.id.tvHouse)
        private val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        private val tvDelay: TextView = view.findViewById(R.id.tvDelay)
        private val tvPaidOn: TextView = view.findViewById(R.id.tvPaidOn)
        private val btnRentInfo: ImageButton = view.findViewById(R.id.btnRentInfo)
        private val btnRemoveRent: ImageButton = view.findViewById(R.id.btnRemoveRent)

        fun bind(rent: Rent) {
            tvTenant.text = rent.tName
            tvHouse.text = rent.hName
            tvAmount.text = CommonUtils.formatNumToText(rent.amount)
            tvDelay.text = CommonUtils.formatNumToText(rent.delay)
            tvPaidOn.text = CommonUtils.getShortDayMonthYearFormatText(rent.paidOn)

            itemView.setOnClickListener { RentActions.showInfo(itemView.context, rent) }
            btnRentInfo.setOnClickListener { RentActions.showInfo(itemView.context, rent) }
            btnRemoveRent.setOnClickListener { RentActions(itemView.context, rent).removeRent() }
        }
    }

    fun filter(query: String) {
        rentsList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.tName.contains(query, ignoreCase = true) ||
                it.hName.contains(query, ignoreCase = true) ||
                it.amount.toString().contains(query, ignoreCase = true)
                // it.paidOn.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}