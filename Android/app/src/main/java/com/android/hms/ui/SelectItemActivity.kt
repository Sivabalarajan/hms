package com.android.hms.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.ui.activities.BaseActivity
import com.android.hms.utils.CommonUtils
import kotlinx.coroutines.*

interface SelectItemDelegate {
    fun handleSelectedItem(id: String)
}

class SelectItemActivity: BaseActivity() {

    private var objectTypeText = ""
    private var selectedItem: CellItem? = null
    private var selectItemTextView: TextView? = null
    private val adapter = SelectItemAdapter()
    private var filteredItemList: List<CellItem> = emptyList()
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemSearchView: SearchView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_item)

        setCustomActionBarView(intent.getStringExtra("title") ?: "")

        objectTypeText = intent.getStringExtra("objectType") ?: ""
        if (objectTypeText.isBlank() || allItemList.isEmpty()) {
            CommonUtils.showMessage(context, "Not able to select", "No ${objectTypeText}s found to select. Please try again later.")
            clearAllValues()
            return
        }
        itemSearchView = findViewById(R.id.item_search_view)
        emptyView = findViewById(R.id.empty_text_view)
        recyclerView = findViewById(R.id.items_recycler_view)

        CommonUtils.initializeRecyclerView(context, recyclerView)
        recyclerView.adapter = adapter

        initSearchView()

        filterAndRefresh()
    }

    private fun setCustomActionBarView(screenTitle: String) {
        val inflater = LayoutInflater.from(context)
        val customView = inflater.inflate(R.layout.activity_select_item_action_bar, null)
        setActionBarCustomView(customView)
        selectItemTextView = customView.findViewById(R.id.select_item_text)
        selectItemTextView?.text = screenTitle
        val selectButton = customView.findViewById<Button>(R.id.select_item_button)
        selectButton.setOnClickListener { selectItem() }
        CommonUtils.setTextViewDrawableColor(selectButton, Color.WHITE, false)
    }

    private fun initSearchView() {
        itemSearchView.isIconified = false
        itemSearchView.setIconifiedByDefault(false)
        itemSearchView.queryHint = "Type a word here to search"
        itemSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                filterAndRefresh() // searchView.query.toString().trim()) // searchText = searchView.query.toString().trim().lowercase()
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                CommonUtils.hideKeyboard(itemSearchView)
                filterAndRefresh() // searchView.query.toString().trim()) // searchText = searchView.query.toString().trim().lowercase()
                return false
            }
        })
        itemSearchView.clearFocus()
    }

    private fun filterAndRefresh() {
        val searchText = itemSearchView.query.toString().trim()
        filteredItemList = if (searchText.isBlank()) {
            hideKeyboard()
            allItemList
        }
        else allItemList.filter { it.titleText.contains(searchText, true) || it.detailText.contains(searchText, true) } // || it.aboutSelf.contains(searchText, true) } // .sortedBy { it.displayName }
        adapter.refreshList(filteredItemList)
        emptyView.visibility = if (filteredItemList.isEmpty()) {
            emptyView.text = "No ${objectTypeText}s found to select. Please change the search criteria or try again later."
            View.VISIBLE
        } else View.GONE
    }

    private fun selectItem() {
        hideKeyboard()
        selectedItem = adapter.selectedItem
        if (selectedItem == null) {
            val prefix = if (objectTypeText.first().lowercase() == "u") "an" else "a"
            CommonUtils.toastMessage(context, "Please select $prefix $objectTypeText")
            return
        }
        finish()
        selectItemDelegate?.handleSelectedItem(selectedItem?.id ?: "")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        super.onBackPressedDispatcher.onBackPressed()
        selectItemDelegate?.handleSelectedItem("")
        clearAllValues()
    }

    companion object {
        private var selectItemDelegate: SelectItemDelegate? = null
        private var allItemList = ArrayList<CellItem>()
        fun clearAllValues() {
            allItemList.clear()
            selectItemDelegate = null
        }

        fun selectFromList(context: Context, itemsList: ArrayList<String>, selectResult: (result: String) -> Unit) {
            val builder = androidx.appcompat.app.AlertDialog.Builder(context) // val builder = MaterialAlertDialogBuilder(context)
            builder.setTitle("Please select")
            // val icons = listOf(R.drawable.ic_topic_24px, R.drawable.ic_comment_icon_24dp, R.drawable.ic_message_icon_24dp)
            val adapter = ArrayAdapter(context, R.layout.single_select_item_dialog, itemsList)
            // val adapter = ArrayAdapterWithIcon(context, itemsList, icons)
            builder.setSingleChoiceItems(adapter, -1) { dialog, index ->
                selectResult(itemsList[index])
                dialog.dismiss()
            }
            builder.show()
        }

        fun initiateSelectHouse(context: Context, delegate: SelectItemDelegate, houses: ArrayList<House>) {
            clearAllValues()
            val title = "Select a house to proceed"
            for (house in houses) {
                val cellItem = CellItem.HouseCellItem(house)
                if (cellItem.id.isNotEmpty()) allItemList.add(cellItem)
            }
            if (allItemList.isEmpty()) {
                CommonUtils.showMessage(context, "No houses", "No house are available to select. Please add a house and try again later.")
                return
            }
            launchActivity(context, delegate, title,  "house")
        }

        private fun launchActivity(context: Context, delegate: SelectItemDelegate, title: String, objectType: String) {
            CommonUtils.isConnectedToNetworkWithWarningMessage(context)
            val intent = Intent(context, SelectItemActivity::class.java)
            intent.putExtra("title", title)
            intent.putExtra("objectType", objectType)
            context.startActivity(intent)
            selectItemDelegate = delegate
        }
    }

    class SelectItemAdapter : RecyclerView.Adapter<SelectItemAdapter.SelectItemViewHolder>(), CoroutineScope by MainScope() {
        private var itemList: List<CellItem> = emptyList()
        var selectedItem: CellItem? = null
        private var layoutInflater: LayoutInflater? = null
        private val context get() = layoutInflater?.context

        fun refreshList(itemList: List<CellItem>) {
            launch {
                this@SelectItemAdapter.itemList = itemList
                notifyDataSetChanged()
            }
        }

        init {
            setHasStableIds(true)
        }

        override fun getItemCount(): Int {
            return itemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectItemViewHolder {
            layoutInflater = LayoutInflater.from(parent.context)
            return SelectItemViewHolder(layoutInflater?.inflate(R.layout.select_items_list_row_layout, parent, false) ?: View(parent.context))
        }

        override fun onBindViewHolder(holder: SelectItemViewHolder, position: Int) {
            if (itemList.isEmpty() || position >= itemList.size) return
            val context = context ?: return
            holder.title.maxWidth = (context.resources.displayMetrics.widthPixels * 0.7).toInt()
            holder.details.maxWidth = holder.title.maxWidth

            val cellItem = itemList[position]
            cellItem.loadImage(holder.image)
            holder.title.text = cellItem.titleText
            holder.details.text = cellItem.detailText
            holder.details.visibility = if (cellItem.detailText.isBlank()) View.GONE else View.VISIBLE
            handleSelection(holder, cellItem)
            holder.itemView.setOnClickListener { onTap(holder, cellItem) }
        }

        fun getItem(position: Int): CellItem? {
            return if (position >= 0 && itemList.size > position) itemList[position] else null
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemViewType(position: Int): Int {
            return position
        }

        private fun handleSelection(holder: SelectItemViewHolder, cellItem: CellItem) {
            val context = context ?: return
            if (selectedItem == cellItem) {
                // holder.parent.background = ContextCompat.getDrawable(context, R.drawable.rounded_corner_send_layout) // ColorDrawable(ThemeColor.colorLighter(context))
                holder.parent.background = CommonUtils.getDrawable(context, R.drawable.rounded_corner_box, R.color.light_color) // ColorDrawable(ThemeColor.colorLighter(context))
                if (!holder.selectRadioButton.isChecked) holder.selectRadioButton.isChecked = true
            }
            else {
                holder.parent.background = ColorDrawable(ContextCompat.getColor(context, R.color.background_color))
                if (holder.selectRadioButton.isChecked) holder.selectRadioButton.isChecked = false
            }
        }

        private fun onTap(holder: SelectItemViewHolder, cellItem: CellItem) {
            val prevSelectedItem = selectedItem
            selectedItem = if (selectedItem == cellItem) null else cellItem
            if (prevSelectedItem != null) notifyItemChanged(itemList.indexOf(prevSelectedItem))
            handleSelection(holder, cellItem)
        }

        class SelectItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val parent: View = view.findViewById(R.id.select_items_layout)
            val selectRadioButton: RadioButton = view.findViewById(R.id.item_select_radio_button)
            val image: ImageButton = view.findViewById(R.id.item_image)
            val title: TextView = view.findViewById(R.id.item_title)
            val details: TextView = view.findViewById(R.id.item_details)
            init { selectRadioButton.isClickable = false }
        }
    }

    open class CellItem {
        var id = ""
        var titleText = ""
        var detailText = ""
        open fun loadImage(imageView: ImageView) { }
        class HouseCellItem(house: House) : CellItem() {
            init {
                id = house.id
                titleText = house.name
                detailText = house.bName
            }
            // override fun loadImage(imageView: ImageView) { MediaUtils.loadGroupPhoto(imageView, group.id, group.photoUpdatedTime) }
        }
    }
}
