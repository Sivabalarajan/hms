package com.android.hms.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.User
import com.android.hms.model.Users

class UserAdapter(
    private var userList: List<User>,
    private val onEditClick: (User) -> Unit,
    private val onRemoveClick: (User) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNameHeader: TextView = itemView.findViewById(R.id.tvUserNameHeader)
        val tvEmailOrPhoneHeader: TextView = itemView.findViewById(R.id.tvEmailOrPhoneHeader)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvEmailOrPhone: TextView = itemView.findViewById(R.id.tvEmailOrPhone)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveUser)
    }

    private val originalList = userList

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            UserViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.tvNameHeader.setOnClickListener { sortBy { it.name } }
            holder.tvEmailOrPhoneHeader.setOnClickListener { sortBy { "${it.email} / ${it.phone}" } }
        } else if (holder is UserViewHolder) {
            val user = userList[position - 1] // Adjust for header
            holder.tvName.text = user.name
            holder.tvEmailOrPhone.text = if (user.email.isEmpty()) user.phone else if (user.phone.isEmpty()) user.email else "${user.email} / ${user.phone}"
            holder.tvName.setOnClickListener { onEditClick(user) }
            if (user.id == Users.currentUser?.id) holder.btnRemove.visibility = View.GONE
            else {
                holder.btnRemove.visibility = View.VISIBLE
                holder.btnRemove.setOnClickListener { onRemoveClick(user) }
            }
        }
    }

    override fun getItemCount() = userList.size + 1 // include header

    fun filter(query: String) {
        userList = if (query.isEmpty()) originalList
        else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true) ||
                        it.phone.contains(query, ignoreCase = true)
                // it.paidOn.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    private fun sortBy(selector: (User) -> String) {
        userList = userList.sortedBy(selector)
        notifyDataSetChanged()
    }
}