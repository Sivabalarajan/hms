package com.android.hms.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Expense
import com.android.hms.ui.ExpenseActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils

class ExpenseReportAdapter(private var expensesList: MutableList<Expense>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val originalList = expensesList
    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    override fun getItemCount(): Int = expensesList.size + 1 // Add 1 for header

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(paexpense: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(paexpense.context).inflate(R.layout.item_expense_header, paexpense, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(paexpense.context).inflate(R.layout.item_expense, paexpense, false)
            ExpenseViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ExpenseViewHolder) {
            val expense = expensesList[position - 1] // Adjust for header
            holder.bind(expense)
        } else if (holder is HeaderViewHolder) {
            holder.bind()
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBuildingHouseHeader: TextView = itemView.findViewById(R.id.tvBuildingHouseHeader)
        private val tvCategoryHeader: TextView = itemView.findViewById(R.id.tvCategoryHeader)
        private val tvAmountHeader: TextView = itemView.findViewById(R.id.tvAmountHeader)
        private val tvPaymentTypeHeader: TextView = itemView.findViewById(R.id.tvPaymentTypeHeader)
        private val tvPaidOnHeader: TextView = itemView.findViewById(R.id.tvPaidOnHeader)

        fun bind() {
            tvBuildingHouseHeader.setOnClickListener { sortBy { it.hName.ifEmpty { it.bName } as Comparable<Any?> } }
            tvCategoryHeader.setOnClickListener { sortBy { it.hName as Comparable<Any?> } }
            tvAmountHeader.setOnClickListener { sortBy { it.amount as Comparable<Any?> } }
            tvPaymentTypeHeader.setOnClickListener { sortBy { it.paidType as Comparable<Any?> } }
            tvPaidOnHeader.setOnClickListener { sortBy { it.paidOn as Comparable<Any?> } }
        }

        private fun sortBy(selector: (Expense) -> Comparable<Any?>) {
            expensesList = expensesList.sortedBy(selector).toMutableList()
            notifyDataSetChanged()
        }
    }

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvBuildingHouse: TextView = view.findViewById(R.id.tvBuildingHouse)
        private val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        private val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        private val tvPaymentType: TextView = view.findViewById(R.id.tvPaymentType)
        private val tvPaidOn: TextView = view.findViewById(R.id.tvPaidOn)
        private val btnExpenseInfo: ImageButton = view.findViewById(R.id.btnExpenseInfo)
        private val btnRemoveExpense: ImageButton = view.findViewById(R.id.btnRemoveExpense)

        fun bind(expense: Expense) {
            tvBuildingHouse.text = expense.hName.ifEmpty { expense.bName }
            tvCategory.text = expense.category
            tvAmount.text = CommonUtils.formatNumToText(expense.amount)
            tvPaymentType.text = expense.paidType
            tvPaidOn.text = CommonUtils.getShortDayMonthYearFormatText(expense.paidOn)

            itemView.setOnClickListener { LaunchUtils.showExpenseActivity(itemView.context, expense) }
            btnExpenseInfo.setOnClickListener { ExpenseActions.showInfo(itemView.context, expense) }
            btnRemoveExpense.setOnClickListener { ExpenseActions(itemView.context, expense).removeExpense() }
        }
    }

    fun filter(query: String) {
        expensesList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.bName.contains(query, ignoreCase = true) ||
                it.hName.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.amount.toString().contains(query, ignoreCase = true)
                // it.paidOn.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}