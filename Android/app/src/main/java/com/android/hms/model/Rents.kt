package com.android.hms.model

import com.android.hms.db.RentsDb
import com.android.hms.utils.Globals
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

data class Rent(var id: String = "", var tId: String = "", var tName: String = "", var hId: String = "", var hName: String = "", var amount: Int = 0,
                var bId: String = "", var bName: String = "", var delay: Int = 0, var paidOn: Long = 0, var notes: String = "")

object Rents {

    private var latePayersList = Collections.synchronizedList(ArrayList<Rent>())

    fun add(rent: Rent, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RentsDb.add(rent) { success, error -> result(success, error) }
    }

    fun update(rent: Rent, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RentsDb.update(rent) { success, error -> result(success, error) }
    }

    fun delete(rent: Rent, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RentsDb.delete(rent) { success, error -> result(success, error) }
    }

    fun getById(id: String): Rent? {
        return RentsDb.getById(id)
    }

    fun getAllFromDb(): ArrayList<Rent> {
        return RentsDb.getAll()
    }

    fun getAllTenants(houseId: String): ArrayList<Pair<String, String>> {
        return RentsDb.getTenants(houseId)
    }

    fun getTotalRentsPaid(houseId: String, from: Long, to: Long): List<Pair<String, Int>> {
        return RentsDb.getTotalRentsPaid(houseId, from, to)
    }

    fun getRentsPaidByTenantGrouped(houseId: String, from: Long, to: Long): List<Triple<String, String, Int>> {
        return RentsDb.getRentsPaidByTenantGrouped(houseId, from, to)
    }

    fun getRentsPaidByTenant(houseId: String, tenantId: String, from: Long, to: Long): Int {
        // TODO: Make this aggregated
        return RentsDb.getRentsPaidByTenant(houseId, tenantId, from, to).sumOf { it.amount }
    }

    fun getRentsByBuildingByHouseByMonth(): ArrayList<Rent> {
        return RentsDb.getAll()
    }

    fun getRentLatePayersByBuilding(buildingId: String): List<Rent> { // <String, String, Int>> { // Triple(it.tenantName, it.houseName, it.delay)
        // return RentsDb.getRentLatePayersByBuilding(buildingId).sortedBy { it.tenantName }
        return getRentLatePayers().filter { it.bId == buildingId } // .sortedBy { it.tName }
    }

    fun getRentLatePayersByHouse(houseId: String): List<Pair<String, Int>> { // tenantName and delay)
        // return RentsDb.getRentLatePayersByHouse(houseId).sortedBy { it.first }
        return getRentLatePayers().filter { it.hId == houseId }.map { Pair(it.tName, it.delay) }
    }

    fun getRentLatePayers(): List<Rent> {
        if (latePayersList.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch { refreshList() }
        }
        return latePayersList.sortedByDescending { it.delay }
    }

    fun getTenantDelayedPayments(tenantId: String): List<Pair<String, Int>> { // Pair(it.houseName, it.delay)
        // return RentsDb.getTenantDelayedPayments(tenantId).sortedBy { it.first }
        return getRentLatePayers().filter { it.tId == tenantId }.map { Pair(it.hName, it.delay) }
    }

    fun getTenantDelayedPaymentsByHouse(tenantId: String, houseId: String): List<Int> { // it.delay)
        // return RentsDb.getTenantDelayedPaymentsByHouse(tenantId, houseId) // .sortedByDescending { it }
        return getRentLatePayers().filter { it.tId == tenantId && it.hId == houseId }.map { it.delay }
    }

    fun refreshList() {
        latePayersList = Collections.synchronizedList(RentsDb.getRentLatePayers())
    }

    fun addLocally(rent: Rent) {
        if (rent.id.isEmpty()) return
        if (rent.delay <= Globals.rentThreshold || latePayersList.indexOfFirst { it.id == rent.id } != -1) return
        latePayersList.add(rent)
        SharedViewModelSingleton.rentPaidEvent.postValue(rent)
    }

    fun updateLocally(rent: Rent) {
        if (rent.id.isEmpty()) return
        if (rent.delay > Globals.rentThreshold) {
            val index = latePayersList.indexOfFirst { it.id == rent.id }
            if (index == -1) addLocally(rent)
            else {
                latePayersList[index] = rent
                SharedViewModelSingleton.rentUpdatedEvent.postValue(rent)
            }
        }
        else deleteLocally(rent)
    }

    fun deleteLocally(rent: Rent) {
        if (rent.id.isEmpty()) return
        val index = latePayersList.indexOfFirst { it.id == rent.id }
        if (index == -1) return
        latePayersList.removeAt(index)
        SharedViewModelSingleton.rentRemovedEvent.postValue(rent)
    }
}