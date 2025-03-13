package com.android.hms.ui.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.ui.HouseActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HouseAdapter(private var housesList: List<House>) : RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    private val originalList = housesList

    override fun getItemCount(): Int {
        return housesList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_house, parent, false)
        return HouseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        holder.bind(housesList[position])
    }

    fun filter(query: String) {
        housesList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        "${it.deposit}".contains(query, ignoreCase = true) ||
                        "${it.rent}".contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    // ViewHolder to represent each item in the RecyclerView
    class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvHouseName)
        private val tvRent: TextView = itemView.findViewById(R.id.tvHouseRent)
        private val tvDeposit: TextView = itemView.findViewById(R.id.tvHouseDeposit)
        // val tvNotes: TextView = itemView.findViewById(R.id.tvHouseNotes)
        // private val btnPayRent: ImageButton = itemView.findViewById(R.id.btnPayRent)
        // private val btnInitiateRepair: ImageButton = itemView.findViewById(R.id.btnInitiateRepair)
        // private val btnAssignOrRemoveTenant: ImageButton = itemView.findViewById(R.id.btnAssignOrRemoveTenant)
        // private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditHouse)

        fun bind(house: House) {
            tvName.text = house.name
            // holder.tvAddress.text = house.address
            tvDeposit.text = CommonUtils.formatNumToText(house.deposit)
            tvRent.text = CommonUtils.formatNumToText(house.rent)
            // holder.tvNotes.text = house.notes`

            CoroutineScope(Dispatchers.Main).launch {
                val defaultColor = itemView.context.getColor(R.color.default_text)
                val highlightColor = itemView.context.getColor(R.color.highlight_color)
                if (house.tId.isEmpty()) {
                    tvName.setTextColor(highlightColor)
                    tvDeposit.setTextColor(defaultColor)
                    tvRent.setTextColor(defaultColor)                }
                else {
                    tvName.setTextColor(defaultColor)
                    tvDeposit.setTextColor(if (house.depositPendingDays() > 0) highlightColor else defaultColor)
                    tvRent.setTextColor(if (house.rentPendingDays() > 0) highlightColor else defaultColor)
                }
            }

            itemView.setOnClickListener { showPopupMenu(house) }
            tvName.setOnClickListener { showPopupMenu(house) }
            // btnEdit.setOnClickListener { editHouse(house) }
            // btnPayRent.setOnClickListener { HouseActions(itemView.context, house).getRentPaid() }
            // btnInitiateRepair.setOnClickListener { HouseActions(itemView.context, house).initiateRepair() }
            // btnAssignOrRemoveTenant.setOnClickListener { tenantAction(house) }
            // btnPayRent.visibility = if (house.tId.isEmpty()) View.GONE else View.VISIBLE

            // updateTenantButton(house)
        }

        private fun tenantAction(house: House) {
            if (house.tJoined > 0) HouseActions(itemView.context, house).makeHouseVacant()
            else HouseActions(itemView.context, house).assignTenant()
        }

        private fun updateTenantMenu(house: House, menu: PopupMenu) {
            if (house.tId.isEmpty()) {
                menu.menu.findItem(R.id.action_assign_tenant).isVisible = true
                menu.menu.findItem(R.id.action_make_house_vacant).isVisible = false
            } else {
                menu.menu.findItem(R.id.action_assign_tenant).isVisible = false
                menu.menu.findItem(R.id.action_make_house_vacant).isVisible = true
            }
        }

        /* private fun updateTenantButton(house: House) {
            if (house.tId.isEmpty()) {
                btnAssignOrRemoveTenant.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.outline_tenant_add_24))
                btnAssignOrRemoveTenant.setColorFilter(ContextCompat.getColor(itemView.context, R.color.assign_tenant_color), PorterDuff.Mode.SRC_IN) // Apply the color filter
                // btnAssignOrRemoveTenant.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.assign_tenant_color))
            } else {
                btnAssignOrRemoveTenant.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.outline_tenant_remove_24))
                btnAssignOrRemoveTenant.setColorFilter(ContextCompat.getColor(itemView.context, R.color.remove_tenant_color), PorterDuff.Mode.SRC_IN) // Apply the color filter
                // btnAssignOrRemoveTenant.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.remove_tenant_color))
            }
        } */

        private fun showPopupMenu(house: House) {
            val popupMenu = PopupMenu(itemView.context, itemView)
            popupMenu.inflate(R.menu.house_actions_popup_menu)
            CommonUtils.setMenuIconsVisible(itemView.context, popupMenu)

            popupMenu.menu.findItem(R.id.action_pay_rent)?.isVisible = house.tId.isNotEmpty()
            popupMenu.menu.findItem(R.id.action_assign_tenant)?.isVisible = house.tId.isEmpty()
            popupMenu.menu.findItem(R.id.action_mark_deposit_as_paid)?.isVisible = house.tId.isNotEmpty() && !house.dPaid
            popupMenu.menu.findItem(R.id.action_make_house_vacant)?.isVisible = house.tId.isNotEmpty()
            // updateTenantMenu(house, popupMenu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_show_house_info -> HouseActions(itemView.context, house).showInfo()
                    R.id.action_show_house_report -> selectHouse(house)
                    R.id.action_mark_deposit_as_paid -> HouseActions(itemView.context, house).markDepositAsPaid()
                    R.id.action_pay_rent -> HouseActions(itemView.context, house).getRentPaid()
                    R.id.action_initiate_repair -> HouseActions(itemView.context, house).initiateRepair()
                    R.id.action_submit_expense -> HouseActions(itemView.context, house).submitExpense()
                    R.id.action_assign_tenant -> HouseActions(itemView.context, house).assignTenant()
                    R.id.action_make_house_vacant -> HouseActions(itemView.context, house).makeHouseVacant()
                    R.id.action_edit_house -> editHouse(house)
                    R.id.action_remove_house -> HouseActions(itemView.context, house).removeHouse()
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
                true
            }
            popupMenu.show()
        }

        private fun selectHouse(house: House) {
            LaunchUtils.showHouseReportsActivity(itemView.context, house)
        }

        private fun editHouse(house: House) {
            LaunchUtils.showHouseActivity(itemView.context, house)
        }
    }
}