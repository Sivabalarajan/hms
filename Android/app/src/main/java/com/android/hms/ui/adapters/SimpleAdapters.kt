package com.android.hms.ui.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.model.User
import com.android.hms.model.Users
import com.android.hms.utils.CommonUtils
import kotlin.collections.ArrayList

class SelectHouseAdapter(private val context: Context) : BaseAdapter() {
    private var houseList: List<House> = ArrayList()
    private var selectedId = ""

    override fun getCount(): Int { return houseList.size }
    override fun getItem(position: Int): House? { return if (houseList.size > position) houseList[position] else null }
    override fun getItemId(position: Int): Long { return position.toLong() }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertViewNew = convertView
        val holder: HouseViewHolder
        if (convertViewNew == null) {
            convertViewNew = LayoutInflater.from(context).inflate(R.layout.select_house_row_layout, parent, false)
            holder = HouseViewHolder(convertViewNew)
            convertViewNew.tag = holder
        }
        else holder = convertViewNew.tag as HouseViewHolder

        if (houseList.isEmpty()) {
            holder.houseName.text = "No houses found."
            return convertViewNew ?: View(context)
        }

        val house = houseList[position]
        holder.houseName.text = house.name
        holder.buildingName.text = house.bName
        holder.selectedView.isChecked = (selectedId == house.id)

        holder.selectedView.setOnClickListener {
            selectedId = house.id
            notifyDataSetChanged()
        }

        return convertViewNew ?: View(context)
    }

    class HouseViewHolder(view: View) {
        val houseName: TextView = view.findViewById(R.id.house_name)
        val buildingName: TextView = view.findViewById(R.id.building_name)
        val selectedView: RadioButton = view.findViewById(R.id.select_view)
    }
}

class ArrayAdapterWithIcon(context: Context, private val items: List<String>, private val images: List<Int>) : ArrayAdapter<String>(context, R.layout.single_select_item_dialog, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<View>(R.id.text_view) as TextView
        textView.text = items[position]
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(images[position], 0, 0, 0)
        textView.compoundDrawablePadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, context.resources.displayMetrics).toInt()
        CommonUtils.setTextViewDrawableColor(textView, ContextCompat.getColor(context, android.R.color.darker_gray), false)
        return view
    }
}

class UserArrayAdapter(context: Context, private val users: List<User>) : ArrayAdapter<User>(context, 0, users) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.single_select_item_dialog, parent, false)
        val nameTextView = view.findViewById<TextView>(R.id.text_view)
        val user = getItem(position) ?: return view
        val email = if (user.email.isEmpty()) "" else " - ${user.email}"
        val phone = if (user.phone.isEmpty()) "" else " - ${user.phone}"
        nameTextView.text = "${user.name}$email$phone"
        return view
    }
}
