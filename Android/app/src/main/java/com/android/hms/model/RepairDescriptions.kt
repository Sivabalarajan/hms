package com.android.hms.model

import com.android.hms.db.RepairDescriptionsDb
import java.util.Collections

data class RepairDescription(var id: String = "", var desc: String = "")

object RepairDescriptions {

    val descriptions: MutableList<String> = Collections.synchronizedList(ArrayList<String>())

    fun add(repairDescription: RepairDescription, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RepairDescriptionsDb.add(repairDescription) { success, error -> result(success, error) }
    }

    fun checkAndAdd(repairDescription: RepairDescription) {
        if (descriptions.indexOf(repairDescription.desc) == -1) {
            descriptions.add(repairDescription.desc)
            add(repairDescription)
        }
    }

    fun update(repairDescription: RepairDescription, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RepairDescriptionsDb.update(repairDescription) { success, error -> result(success, error) }
    }

    fun delete(repairDescription: RepairDescription, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        RepairDescriptionsDb.delete(repairDescription) { success, error -> result(success, error) }
    }

    fun getById(id: String): RepairDescription? {
        return RepairDescriptionsDb.getById(id)
    }

    fun getByName(name: String): RepairDescription? {
        return RepairDescriptionsDb.getByName(name)
    }

    fun getAll(): ArrayList<RepairDescription> {
        return RepairDescriptionsDb.getAll()
    }

    fun refreshList() {
        descriptions.clear()
        RepairDescriptionsDb.getAll().forEach { descriptions.add(it.desc) }
        val duplicates = descriptions.groupBy { it }
            .filter { it.value.size > 1 }
            .keys
        duplicates.forEach { delete(getByName(it) ?: return@forEach) }
    }

    fun addLocally(repairDescription: RepairDescription) {
        if (repairDescription.id.isEmpty()) return
        if (descriptions.indexOf(repairDescription.desc) == -1) descriptions.add(repairDescription.desc)
    }

    fun updateLocally(repairDescription: RepairDescription) {
        if (repairDescription.id.isEmpty()) return
        addLocally(repairDescription)
        // this should not happen
    }

    fun deleteLocally(repairDescription: RepairDescription) {
        if (repairDescription.id.isEmpty()) return
        if (descriptions.indexOf(repairDescription.desc) != -1) descriptions.remove(repairDescription.desc)
    }
}