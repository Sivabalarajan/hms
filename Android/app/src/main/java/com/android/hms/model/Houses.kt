package com.android.hms.model

import com.android.hms.db.HousesDb
import com.android.hms.utils.CommonUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

data class House(var id: String = "", var name: String = "", var tId: String = "", var tName: String = "", var bId: String = "", var bName: String = "", var rent: Int = 0, // rPaid -> rent last paid :-)
                 var deposit: Int = 0, var dPaid: Boolean = false, var notes: String = "", var tJoined: Long = 0, var tLeft: Long = 0, var rPaid: Long = 0, var rRevised: Long = 0) {

    fun depositPendingDays(): Int {
        if (tLeft != 0L || tJoined == 0L) return -1 // tenant to be assigned
        if (dPaid) return 0
        return CommonUtils.calculateDiffDays(tJoined) + 1
    }

    fun rentPendingDays(): Int {
        if (tLeft != 0L || tJoined == 0L) return -1 // tenant to be assigned
        return CommonUtils.calculateDiffDays((if (rPaid == 0L) tJoined else rPaid) + CommonUtils.oneMonthTime()) + 1
    }

    fun vacantDays(): Int {
        if (tLeft == 0L && tJoined == 0L && rPaid == 0L) return -1 // tenant to be assigned
        if (tLeft == 0L) return 0
        return CommonUtils.calculateDiffDays(tLeft)
    }

    fun rentRevisedDays(): Int {
        if (rRevised == 0L) return 0
        return CommonUtils.calculateDiffDays(rRevised + CommonUtils.oneMonthTime()) + 1
    }
}

data class BuildingHouse(val id: String, var name: String = "", var houses: ArrayList<House> = ArrayList())

object Houses {

    private var houses = Collections.synchronizedList(ArrayList<House>())
    private var housesMapList: MutableMap<String, House> = emptyMap<String, House>().toMutableMap()

    fun add(house: House, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        HousesDb.add(house) { success, error -> result(success, error) }
    }

    fun update(house: House, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        HousesDb.update(house) { success, error -> result(success, error) }
    }

    fun delete(house: House, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        HousesDb.delete(house) { success, error -> result(success, error) }
    }

    fun deleteByBuilding(buildingId: String) {
        HousesDb.deleteByBuilding(buildingId)
    }

    fun makeRented(house: House, tenant: User, rent: Int, deposit: Int, isHouseDepositPaid: Boolean, notes: String, joinedDate: Long) {
        house.tId = tenant.id
        house.tName = tenant.name
        house.tJoined = joinedDate
        house.notes = notes
        house.tLeft = 0L
        house.rPaid = 0L
        house.dPaid = isHouseDepositPaid
        if (house.rent != rent || house.rRevised == 0L) {
            house.rent = rent
            house.deposit = deposit
            house.rRevised = joinedDate
        }
        update(house)
    }

    fun reviseRent(house: House, revisedRent: Int, rentRevisedDate: Long) {
        house.rent = revisedRent
        house.rRevised = rentRevisedDate
        HousesDb.update(house)
    }

    fun makeVacant(house: House, leftDate: Long) {
        house.tId = ""
        house.tName = ""
        house.tLeft = leftDate
        house.tJoined = 0L
        // house.rPaid = 0L
        update(house)
    }

    fun getById(id: String): House? {
        return housesMapList[id]
        // return getAll().firstOrNull { it.id == id }
    }

    fun getByNameInBuilding(name: String, buildingId: String): House? {
        return getAll().firstOrNull { it.name == name && it.bId == buildingId }
    }

    fun getAll(): ArrayList<House> {
        if (houses.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch { refreshList() }
        }
        return ArrayList(houses)
    }

    fun getHousesCount(buildingId: String): Int {
        return getAll().filter { it.bId == buildingId }.size
    }

    fun getAllByBuilding(buildingId: String): List<House> {
        return getAll().filter { it.bId == buildingId }
    }

    fun groupHousesByBuilding(): ArrayList<BuildingHouse> {
        val sortedHouses = getAll().sortedWith(compareBy<House> { it.bName }.thenBy { it.name })
        return ArrayList(sortedHouses.groupBy { it.bId to it.bName }
            .map { (buildingInfo, houseList) ->
                val (buildingId, buildingName) = buildingInfo
                BuildingHouse(buildingId, buildingName, ArrayList(houseList))
            })
    }

    fun getAllByBuildingName(buildingName: String): ArrayList<House> {
        return ArrayList(getAll().filter { it.bName == buildingName })
    }

    fun getVacantHousesByBuilding(buildingId: String): ArrayList<House> {
        return ArrayList(getAll().filter { it.tId.isEmpty() && it.bId == buildingId }.sortedBy { it.rPaid }) // sortedWith(compareBy<House> { it.rPaid }.thenBy { it.name }))
    }

    fun getVacantHouses(): ArrayList<House> {
        return ArrayList(getAll().filter { it.tId.isEmpty() }.sortedBy { it.rPaid }) // sortedWith(compareBy<House> { it.bName }.thenBy { it.rPaid }))
    }

    fun getDepositNotPaidTenants(): ArrayList<House> {
        return ArrayList(getAll().filter { it.tId.isNotEmpty() && !it.dPaid }.sortedBy { it.tJoined })
    }

    fun getDepositNotPaidTenantsByBuilding(buildingId: String): ArrayList<House> {
        return ArrayList(getAll().filter { it.tId.isNotEmpty() && !it.dPaid && it.bId == buildingId }.sortedBy { it.tJoined })
    }

    fun getRentNotPaidTenants(): ArrayList<House> {
        val lastMonthTime = CommonUtils.getLastMonthTime()
        return ArrayList(getAll().filter { it.tId.isNotEmpty() && it.rPaid < lastMonthTime }.sortedBy { it.rPaid }) // sortedWith(compareBy<House> { it.rPaid }.thenBy { it.tName }))
    }

    fun getRentNotPaidTenantsByBuilding(buildingId: String): ArrayList<House> {
        val lastMonthTime = CommonUtils.getLastMonthTime()
        return ArrayList(getAll().filter { it.tId.isNotEmpty() && it.rPaid < lastMonthTime && it.bId == buildingId }.sortedBy { it.rPaid })  // sortedWith(compareBy<House> { it.name }.thenBy { it.rPaid }))
    }

    fun getTenantHouses(tenantId: String): ArrayList<House> {
        return ArrayList(getAll().filter { it.tId == tenantId }.sortedBy { it.name })
    }

    fun refreshList() {
        houses = Collections.synchronizedList(HousesDb.getAll())
        housesMapList = houses.associateBy { it.id }.toMutableMap()
    }

    fun addLocally(house: House) {
        if (house.id.isEmpty()) return
        if (houses.indexOfFirst { it.id == house.id } != -1) return
        houses.add(house)
        housesMapList[house.id] = house
        SharedViewModelSingleton.houseAddedEvent.postValue(house)
    }

    fun updateLocally(house: House) {
        if (house.id.isEmpty()) return
        val index = houses.indexOfFirst { it.id == house.id }
        if (index == -1) addLocally(house)
        else {
            houses[index] = house
            housesMapList[house.id] = house
            SharedViewModelSingleton.houseUpdatedEvent.postValue(house)
        }
    }

    fun deleteLocally(house: House) {
        if (house.id.isEmpty()) return
        val index = houses.indexOfFirst { it.id == house.id }
        if (index == -1) return
        houses.removeAt(index)
        housesMapList.remove(house.id)
        SharedViewModelSingleton.houseRemovedEvent.postValue(house)
    }
}