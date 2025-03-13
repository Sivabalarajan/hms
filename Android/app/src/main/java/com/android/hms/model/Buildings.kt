package com.android.hms.model

import com.android.hms.db.BuildingsDb
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

data class Building(var id: String = "", var name: String = "", var address: String = "", var area: String = "", var notes: String = "")

object Buildings {

    private var buildings = Collections.synchronizedList(ArrayList<Building>())

    fun add(building: Building, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        BuildingsDb.add(building) { success, error ->
            result(success, error)
        }
    }

    fun update(building: Building, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        BuildingsDb.update(building) { success, error -> result(success, error) }
    }

    fun delete(building: Building, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        BuildingsDb.delete(building) { success, error ->
            result(success, error)
            if (success) Houses.deleteByBuilding(building.id) // delete all the houses associated with this building
        }
    }

    fun getById(id: String): Building? {
        return getAll().firstOrNull { it.id == id }
    }

    fun getByName(name: String): Building? {
        return getAll().firstOrNull { it.name == name }
    }

    fun getAll(): ArrayList<Building> {
        if (buildings.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch { refreshList() }
        }
        return ArrayList(buildings)
    }

    fun refreshList() {
        buildings = Collections.synchronizedList(BuildingsDb.getAll())
    }

    fun addLocally(building: Building) {
        if (building.id.isEmpty()) return
        if (buildings.indexOfFirst { it.id == building.id } != -1) return
        buildings.add(building)
        SharedViewModelSingleton.buildingAddedEvent.postValue(building)
    }

    fun updateLocally(building: Building) {
        if (building.id.isEmpty()) return
        val index = buildings.indexOfFirst { it.id == building.id }
        if (index == -1) addLocally(building)
        else {
            buildings[index] = building
            SharedViewModelSingleton.buildingUpdatedEvent.postValue(building)
        }
    }

    fun deleteLocally(building: Building) {
        if (building.id.isEmpty()) return
        val index = buildings.indexOfFirst { it.id == building.id }
        if (index == -1) return
        buildings.removeAt(index)
        SharedViewModelSingleton.buildingRemovedEvent.postValue(building)
    }
}