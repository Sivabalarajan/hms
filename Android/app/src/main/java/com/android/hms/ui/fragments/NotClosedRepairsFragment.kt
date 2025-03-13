package com.android.hms.ui.fragments

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.android.hms.ui.activities.BaseActivity
import com.android.hms.ui.adapters.RepairReportAdapter
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.launch

class NotClosedRepairsFragment: BaseFragment() {

    private lateinit var recyclerView: RecyclerView
    private var notClosedRepairsList = ArrayList<Repair>()
    private var notClosedRepairsAdapter: RepairReportAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_not_closed_repairs_list, container, false)

        (activity as? BaseActivity)?.setActionBarView("Not Closed Repairs")

        recyclerView = view.findViewById(R.id.not_closed_repairs_recycler)
        // recyclerView.layoutManager = GridLayoutManager(context, 4)
        recyclerView.layoutManager = LinearLayoutManager(context) // GridLayoutManager(context, columnCount)

        val context = context ?: return view
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch {
            notClosedRepairsList = Repairs.getAllNotClosed()
            if (notClosedRepairsList.isEmpty()) {
                CommonUtils.toastMessage(context, "No open repairs are available. Please add and try again later.")
                progressBar.dismiss()
                return@launch
            }
            notClosedRepairsAdapter = RepairReportAdapter(notClosedRepairsList)
            recyclerView.adapter = notClosedRepairsAdapter
            setObservers()
            // (activity as? BaseActivity)?.initSearchView(view.findViewById(R.id.searchView), ::filter)
            progressBar.dismiss()
        }

        return view
    }

    override fun filter(searchText: String) {
        notClosedRepairsAdapter?.filter(searchText)
    }

    private fun setObservers() {
        SharedViewModelSingleton.repairInitiatedEvent.observe(viewLifecycleOwner) { repair -> updateAdapterForAddition(repair) }
        SharedViewModelSingleton.repairUpdatedEvent.observe(viewLifecycleOwner) { repair -> updateAdapterForChange(repair) }
        SharedViewModelSingleton.repairRemovedEvent.observe(viewLifecycleOwner) { repair -> updateAdapterForRemove(repair) }
    }

    private fun updateAdapterForAddition(repair: Repair) {
        if (repair.status == Repairs.statuses.last()) return
        if (notClosedRepairsList.indexOfFirst { it.id == repair.id } != -1) return
        notClosedRepairsList.add(repair)
        notClosedRepairsAdapter?.notifyItemInserted(notClosedRepairsList.size) // header row is included
    }

    private fun updateAdapterForChange(repair: Repair) {
        if (repair.status == Repairs.statuses.last()) {
            updateAdapterForRemove(repair)
            return
        }
        val index = notClosedRepairsList.indexOfFirst { it.id == repair.id }
        if (index == -1) {
            updateAdapterForAddition(repair)
            return
        }
        notClosedRepairsList[index] = repair
        notClosedRepairsAdapter?.notifyItemChanged(index + 1) // header row is included
    }

    private fun updateAdapterForRemove(repair: Repair) {
        val index = notClosedRepairsList.indexOfFirst { it.id == repair.id }
        if (index == -1) return
        notClosedRepairsList.removeAt(index)
        notClosedRepairsAdapter?.notifyItemRemoved(index + 1) // header row is included
    }
}