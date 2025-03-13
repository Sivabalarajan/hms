package com.android.hms.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Rent
import com.android.hms.ui.RentActions
import com.android.hms.utils.CommonUtils

class RentMonthlyAdapter(private val monthData: Map<String, Pair<Double, List<Rent>>>) : RecyclerView.Adapter<RentMonthlyAdapter.MonthViewHolder>() {

    class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val monthTextView: TextView = itemView.findViewById(R.id.monthTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_repair_category_month_row, parent, false)
        return MonthViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val month = monthData.keys.elementAt(position)
        val amountData = monthData[month] ?: return

        holder.monthTextView.text = month
        holder.amountTextView.text = CommonUtils.formatNumToText(amountData.first)
        holder.amountTextView.setOnClickListener {
            val rentList = amountData.second
            if (rentList.isNotEmpty()) RentActions.showSummaryInfo(holder.itemView.context, rentList)
        }
    }

    override fun getItemCount() = monthData.size
}