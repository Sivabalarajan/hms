package com.android.hms.ui.adapters

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.android.hms.R
import com.android.hms.model.BuildingHouse
import com.android.hms.model.House
import com.android.hms.utils.CommonUtils

class BuildingsHousesAdapter(
    private val context: Context,
    private var buildingHouses: List<BuildingHouse>,
    private val onBuildingAction: (BuildingHouse, BuildingActionType) -> Unit,
    private val onHouseAction: (House, HouseActionType) -> Unit
) : BaseExpandableListAdapter() {

    private val originalList = buildingHouses

    private lateinit var btnAssignOrRemoveTenant: ImageButton

    enum class BuildingActionType { SELECT, INITIATE_MAINTENANCE, ADD_HOUSE, EDIT_BUILDING}
    enum class HouseActionType { SELECT, INFO, INITIATE_MAINTENANCE, PAY_RENT, TENANT, EDIT_HOUSE }

    override fun getGroup(groupPosition: Int): Any = buildingHouses[groupPosition]
    override fun getChild(groupPosition: Int, childPosition: Int): Any = buildingHouses[groupPosition].houses[childPosition]
    override fun getGroupCount(): Int = buildingHouses.size
    override fun getChildrenCount(groupPosition: Int): Int = buildingHouses[groupPosition].houses.size
    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true
    override fun hasStableIds(): Boolean = true
    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()
    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

     override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
         val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.building_item_elv, parent, false)
         val building = getGroup(groupPosition) as BuildingHouse

         val buildingName = view.findViewById<TextView>(R.id.tvBuildingName)
         val btnInitiateRepair = view.findViewById<ImageView>(R.id.btnInitiateRepair)
         val btnAddHouse = view.findViewById<ImageView>(R.id.btnAddHouse)
         val btnEditBuilding = view.findViewById<ImageView>(R.id.btnEditBuilding)

         buildingName.text = building.name
         buildingName.setOnClickListener { onBuildingAction(building, BuildingActionType.SELECT) }
         btnInitiateRepair.setOnClickListener { onBuildingAction(building, BuildingActionType.INITIATE_MAINTENANCE) }
         btnAddHouse.setOnClickListener { onBuildingAction(building, BuildingActionType.ADD_HOUSE) }
         btnEditBuilding.setOnClickListener { onBuildingAction(building, BuildingActionType.EDIT_BUILDING) }

         /* Optionally handle expand/collapse indicator visibility here if needed
        val indicator = view.findViewById<ImageView>(R.id.expandCollapseIndicator)
        if (isExpanded) {
            indicator.setImageResource(R.drawable.round_expand_less_24)  // Your custom collapse icon
        } else {
            indicator.setImageResource(R.drawable.round_expand_more_24)    // Your custom expand icon
        } */

         return view
     }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.house_item_elv, parent, false)
        val house = getChild(groupPosition, childPosition) as House

        val houseName = view.findViewById<TextView>(R.id.tvHouseName)
        val houseInfo = view.findViewById<TextView>(R.id.tvHouseInfo)
        val btnPayRent = view.findViewById<ImageButton>(R.id.btnPayRent)
        val btnInitiateRepair = view.findViewById<ImageButton>(R.id.btnInitiateRepair)
        val btnEditHouse = view.findViewById<ImageButton>(R.id.btnEditHouse)

        btnAssignOrRemoveTenant = view.findViewById(R.id.btnAssignOrRemoveTenant)
        updateTenantButton(house)

        houseName.text = house.name
        houseInfo.text = if (house.rPaid > 0) "Last Rent Paid: ${CommonUtils.getMonthYearOnlyFormatText(house.rPaid)}"
        else if (house.tId.isEmpty()) "Tenant to be assigned" else "${house.tName} yet to pay the rent"

        houseName.setOnClickListener { onHouseAction(house, HouseActionType.SELECT) }
        houseInfo.setOnClickListener { onHouseAction(house, HouseActionType.INFO) }
        btnPayRent.setOnClickListener { onHouseAction(house, HouseActionType.PAY_RENT) }
        btnInitiateRepair.setOnClickListener { onHouseAction(house, HouseActionType.INITIATE_MAINTENANCE) }
        btnAssignOrRemoveTenant.setOnClickListener { onHouseAction(house, HouseActionType.TENANT) }
        btnEditHouse.setOnClickListener { onHouseAction(house, HouseActionType.EDIT_HOUSE) }

        btnPayRent.visibility = if (house.tId.isEmpty()) View.GONE else View.VISIBLE

        return view
    }

    fun filter(query: String) {
        buildingHouses = if (query.isEmpty()) originalList
        else {
            originalList.filter { building ->
                building.name.contains(query, ignoreCase = true) ||
                        building.houses.find { it.name.contains(query, ignoreCase = true) } != null
                // it.paidOn.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    private fun updateTenantButton(house: House) {
        if (house.tId.isEmpty()) {
            btnAssignOrRemoveTenant.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.outline_tenant_add_24))
            btnAssignOrRemoveTenant.setColorFilter(ContextCompat.getColor(context, R.color.assign_tenant_color), PorterDuff.Mode.SRC_IN) // Apply the color filter
            // btnAssignOrRemoveTenant.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.assign_tenant_color))
        }
        else {
            btnAssignOrRemoveTenant.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.outline_tenant_remove_24))
            btnAssignOrRemoveTenant.setColorFilter(ContextCompat.getColor(context, R.color.remove_tenant_color) , PorterDuff.Mode.SRC_IN) // Apply the color filter
            // btnAssignOrRemoveTenant.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.remove_tenant_color))
        }
    }
}
