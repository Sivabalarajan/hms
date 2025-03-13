package com.android.hms.model

import com.android.hms.db.RepairsDb
import com.android.hms.utils.CommonUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

data class Repair(var id: String = "", var desc: String = "", var notes: String = "", var amount: Double = 0.0, var tPaid: Boolean = true, var paidType: String = "",
                  var bId: String = "", var bName: String = "", var hId: String = "", var hName: String = "", var status: String = "", var tId: String = "",
                  var tName: String = "", var raisedOn: Long = 0, var fixedOn: Long = 0, var paidOn: Long = 0, var raisedBy: String = "", var fixedBy: String = "")

object Repairs {

    // enum class Status(val value: String) { OPEN("O"), IN_PROGRESS("I"), BLOCKED("B"), CLOSED("C")  }
    val statuses = listOf("New", "Started", "Blocked", "Fixed", "Paid", "Closed") // ENSURE LAST AS 'Closed' AND FIRST AS 'New'

    private var notClosedRepairs = Collections.synchronizedList(ArrayList<Repair>())

    fun add(repair: Repair, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RepairsDb.add(repair) { success, error -> result(success, error) }
    }

    fun update(repair: Repair, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RepairsDb.update(repair) { success, error -> result(success, error) }
    }

    fun delete(repair: Repair, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RepairsDb.delete(repair) { success, error -> result(success, error) }
    }

    fun makeInProgress(repair: Repair) {
        repair.status = statuses[1]
        RepairsDb.update(repair)
    }

    fun makeBlocked(repair: Repair) {
        repair.status = statuses[2]
        RepairsDb.update(repair)
    }

    fun makeClosed(repair: Repair) {
        repair.status = statuses.last()
        RepairsDb.update(repair)
    }

    fun getByIdFromDb(id: String): Repair? {
        return RepairsDb.getById(id)
    }

    fun getAllFromDb(): ArrayList<Repair> {
        return RepairsDb.getAll()
    }

    fun getAllOpenOverWeek(): ArrayList<Repair> {
        val lastWeekTime = CommonUtils.getLastWeekTime()
        return ArrayList(getAllNotClosed().filter { it.raisedOn < lastWeekTime && it.fixedOn == 0L }.sortedBy { it.raisedOn })
    }

    fun getAllNotPaidOverWeek(): ArrayList<Repair> {
        val lastWeekTime = CommonUtils.getLastWeekTime()
        return ArrayList(getAllNotClosed().filter { it.fixedOn != 0L && it.fixedOn < lastWeekTime && it.paidOn == 0L }.sortedBy { it.fixedOn })
    }

    fun getAllNotFixed(): ArrayList<Repair> {
        return ArrayList(getAllNotClosed().filter { it.fixedOn == 0L }.sortedBy { it.raisedOn })
    }

    fun getAllFixedButNotPaid(): ArrayList<Repair> {
        return ArrayList(getAllNotClosed().filter { it.fixedOn > 0L && it.paidOn == 0L }.sortedBy { it.fixedOn })
    }

    fun getAllPaidButNotClosed(): ArrayList<Repair> {
        return ArrayList(getAllNotClosed().filter { it.paidOn > 0L }.sortedBy { it.paidOn })
    }

    fun getAllNotClosed(): ArrayList<Repair> {
        if (notClosedRepairs.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch { refreshList() }
        }
        return ArrayList(notClosedRepairs.sortedBy { it.raisedOn })
    }

    fun getAllNotClosedByBuilding(buildingId: String): ArrayList<Repair> {
        return ArrayList(getAllNotClosed().filter { it.bId == buildingId }.sortedBy { it.raisedOn })
    }

    fun getAllNotClosedByHouse(houseId: String): ArrayList<Repair> {
        return ArrayList(getAllNotClosed().filter { it.hId == houseId }.sortedBy { it.raisedOn })
    }

    fun getAllByBuilding(buildingId: String): ArrayList<Repair> {
        return RepairsDb.getAllByBuilding(buildingId)
    }

    fun getAllByHouse(houseId: String): ArrayList<Repair> {
        return RepairsDb.getAllByHouse(houseId)
    }

    fun getTenantPaid(): ArrayList<Repair> {
        return RepairsDb.getTenantPaid(true)
    }

    fun getOwnerPaid(): ArrayList<Repair> {
        return RepairsDb.getTenantPaid(false)
    }

    fun getByHouseByCategoryByMonth(): ArrayList<Repair> {
        // TODO: do aggregation by category, house and building
        return RepairsDb.getAll()
    }

    fun getAllFixedToday(): ArrayList<Repair> {
        return RepairsDb.getAllFixedToday()
    }

    fun getAllPaidToday(): ArrayList<Repair> {
        return RepairsDb.getAllPaidToday()
    }

    fun getAllByTenantId(tenantId: String): ArrayList<Repair> {
        return RepairsDb.getAllByTenantId(tenantId)
    }

    fun refreshList() {
        notClosedRepairs = Collections.synchronizedList(RepairsDb.getAllNotClosed())
    }

    fun addLocally(repair: Repair) {
        if (repair.id.isEmpty()) return
        if (repair.status == statuses.last() || notClosedRepairs.indexOfFirst { it.id == repair.id } != -1) return
        notClosedRepairs.add(repair)
        SharedViewModelSingleton.repairInitiatedEvent.postValue(repair)
    }

    fun updateLocally(repair: Repair) {
        if (repair.id.isEmpty()) return
        if (repair.status != statuses.last()) {
            val index = notClosedRepairs.indexOfFirst { it.id == repair.id }
            if (index == -1) addLocally(repair)
            else {
                notClosedRepairs[index] = repair
                SharedViewModelSingleton.repairUpdatedEvent.postValue(repair)
            }
        } else deleteLocally(repair)
    }

    fun deleteLocally(repair: Repair) {
        if (repair.id.isEmpty()) return
        val index = notClosedRepairs.indexOfFirst { it.id == repair.id }
        if (index == -1) return
        notClosedRepairs.removeAt(index)
        SharedViewModelSingleton.repairRemovedEvent.postValue(repair)
    }

    fun getByIdLocally(id: String): Repair? {
        return notClosedRepairs.firstOrNull { it.id == id }
    }
}