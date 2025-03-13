package com.android.hms.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Repairs
import com.android.hms.model.Houses
import com.android.hms.model.Rents
import com.android.hms.ui.fragments.DashboardType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import kotlinx.coroutines.withContext

data class DashboardItem(val type: DashboardType, val title: String, val description: String)

class DashboardAdapter (private var items: List<DashboardItem>, private val onItemClick: (DashboardType) -> Unit) : RecyclerView.Adapter<DashboardAdapter.ViewHolder>() {

    private val originalList = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dashboard_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
//        holder.tvTitle.text = item.title
//        holder.tvDescription.text = item.description
        holder.tvTitle.text =  getHighlightedText(holder.tvTitle.context, item.title)
        holder.tvDescription.text = getHighlightedText(holder.tvDescription.context, item.description)
        holder.tvTotal.text ="Total: [Calculating... please wait...]"
        updateTotal(holder.tvTotal, item.type)
        holder.itemView.setOnClickListener { onItemClick(item.type) }
    }

    override fun getItemCount() = items.size

    private fun updateTotal(tvTotal: TextView, dashboardType: DashboardType) {
        CoroutineScope(Dispatchers.Main).launch {
            val total = withContext(Dispatchers.IO) {
                when (dashboardType) {
                    DashboardType.DEPOSIT_NOT_PAID_TENANTS -> Houses.getDepositNotPaidTenants()
                    DashboardType.RENT_NOT_PAID_TENANTS -> Houses.getRentNotPaidTenants()
                    DashboardType.VACANT_HOUSES -> Houses.getVacantHouses()
                    DashboardType.RENT_LATE_PAYERS -> Rents.getRentLatePayers()
                    DashboardType.REPAIRS_OPEN_FOR_OVER_WEEK -> Repairs.getAllOpenOverWeek()
                    DashboardType.REPAIRS_NOT_PAID_FOR_OVER_WEEK -> Repairs.getAllNotPaidOverWeek()
                    DashboardType.REPAIRS_NOT_FIXED -> Repairs.getAllNotFixed()
                    DashboardType.REPAIRS_FIXED_BUT_NOT_PAID -> Repairs.getAllFixedButNotPaid()
                    DashboardType.REPAIRS_PAID_BUT_NOT_CLOSED -> Repairs.getAllPaidButNotClosed()
                    DashboardType.REPAIRS_ALL_NOT_CLOSED -> Repairs.getAllNotClosed()
                }.size
            }
            tvTotal.text = "Total: $total"
        }
    }

    private var queryString = ""
    fun filter(query: String) {
        queryString = query

        items = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }.toMutableList()
        }

        notifyDataSetChanged()
    }

    private fun getHighlightedText(context: Context, text: String): SpannableString {
        val spannable = SpannableString(text)
        val start = text.indexOf(queryString, ignoreCase = true)
        if (start >= 0) {
            val end = start + queryString.length
            spannable.setSpan(BackgroundColorSpan(context.getColor(R.color.lighter_gray_medium)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
    }
}
