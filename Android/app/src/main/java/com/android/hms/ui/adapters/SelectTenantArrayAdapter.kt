package com.android.hms.ui.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.android.hms.R
import com.android.hms.model.User

class SelectTenantArrayAdapter(context: Context, private val tenants: List<User>) : ArrayAdapter<User>(context, R.layout.single_select_item_dialog, tenants) {
    private var selectedPosition: Int = -1
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)

        val tenant = tenants[position]
        val email = if (tenant.email.isEmpty()) "" else " - ${tenant.email}"
        val phone = if (tenant.phone.isEmpty()) "" else " - ${tenant.phone}"
        val textView = view.findViewById<View>(R.id.text_view) as TextView
        textView.text = "${tenant.name}$email$phone"

        // textView.setCompoundDrawablesRelativeWithIntrinsicBounds(images[position], 0, 0, 0)
//        textView.compoundDrawablePadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, context.resources.displayMetrics).toInt()
//        CommonUtils.setTextViewDrawableColor(textView, ContextCompat.getColor(context, android.R.color.darker_gray), false)

        // Highlight the selected item
        if (position == selectedPosition) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_item)) // Highlight color
//            textView.setTextColor(ContextCompat.getColor(context, R.color.selected_text)) // Text color for selected
        } else {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.default_item)) // Default background
//            textView.setTextColor(ContextCompat.getColor(context, R.color.default_text)) // Default text color
        }

        return view
    }

    // Method to set selected position and refresh the list
    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged() // Refresh the ListView
    }
}
