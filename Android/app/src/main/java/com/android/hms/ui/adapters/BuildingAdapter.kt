package com.android.hms.ui.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.android.hms.R
import com.android.hms.model.Building
import com.android.hms.model.Houses
import com.android.hms.ui.BuildingActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.LaunchUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuildingAdapter(private var buildingList: List<Building>): RecyclerView.Adapter<BuildingAdapter.BuildingViewHolder>() {

    private val originalList = buildingList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        // Inflating the layout for the individual building item
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_building, parent, false)
        return BuildingViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        holder.bind(buildingList[position])
    }

    override fun getItemCount(): Int {
        return buildingList.size
    }

    fun filter(query: String) {
        buildingList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    // ViewHolder to represent each item in the RecyclerView
    class BuildingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvBuildingName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvBuildingAddress)
//        val tvArea: TextView = itemView.findViewById(R.id.tvBuildingArea)  // Added area
//        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditBuilding)
//        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveBuilding)

        fun bind(building: Building) {
            tvName.text = building.name
            tvAddress.text = building.address

            CoroutineScope(Dispatchers.Main).launch {
                val housesList = withContext(Dispatchers.IO) { Houses.getAllByBuilding(building.id) }
                val nHouses = housesList.size
                if (nHouses == 0) tvName.setTextColor(itemView.context.getColor(R.color.highlight_color))
                else {
                    tvName.text = "${building.name} ($nHouses)"
                    CoroutineScope(Dispatchers.Main).launch {
                        var defaultColor = housesList.none { it.tId.isEmpty() }
                        if (defaultColor) defaultColor = housesList.none { !it.dPaid }
                        tvName.setTextColor(itemView.context.getColor(if (defaultColor) R.color.default_text else R.color.highlight_color))
                    }
                }
            }

            // tvArea.text = building.area
            itemView.setOnClickListener { showPopupMenu(building) }
            tvName.setOnClickListener { showPopupMenu(building) }

//            holder.tvName.setOnClickListener { onSelectClick(building) }
//            holder.btnEdit.setOnClickListener { onEditClick(building) }
//            holder.btnRemove.setOnClickListener { onRemoveClick(building) }
        }

        private fun showPopupMenu(building: Building) {
            val popupMenu = PopupMenu(itemView.context, itemView)
            popupMenu.inflate(R.menu.building_actions_popup_menu)
            CommonUtils.setMenuIconsVisible(itemView.context, popupMenu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_show_building_report -> LaunchUtils.showBuildingReportsActivity(itemView.context, building.id, building.name)
                    R.id.action_initiate_repair -> LaunchUtils.showRepairActivity(itemView.context, building.id, building.name)
                    R.id.action_submit_expense -> LaunchUtils.showExpenseActivity(itemView.context, building.id, building.name)
                    R.id.action_view_houses -> SharedViewModelSingleton.selectedBuildingEvent.postValue(building)
                    R.id.action_add_house -> LaunchUtils.showHouseActivity(itemView.context, building.id, building.name)
                    R.id.action_edit_building -> LaunchUtils.showBuildingActivity(itemView.context, building.id)
                    R.id.action_remove_building -> BuildingActions.removeBuilding(itemView.context, building)
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }
                true
            }
            popupMenu.show()
        }
    }
}
