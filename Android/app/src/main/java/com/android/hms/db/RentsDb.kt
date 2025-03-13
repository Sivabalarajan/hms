package com.android.hms.db

import com.android.hms.model.Rent
import com.android.hms.model.Rents
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

object RentsDb : DbBase() {
    private const val TAG = "RentsDB"
    override val collectionName = if (Globals.gDevelopmentMode) "dev_rents" else "rents"

    fun add(rent: Rent, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .add(rent)
            .addOnSuccessListener { documentReference ->
                launch {
                    rent.id = documentReference.id
                    update(rent) // update with newly generated id
                    result(true, "")
                    CommonUtils.printMessage("Rent ${rent.amount}(${rent.id}) by ${rent.tName}is paid")
                }
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to pay rent by ${rent.tName}: $e")
            }
    }

    fun add(rents: ArrayList<Rent>) {
        for (rent in rents) {
            Connection.db.collection(collectionName)
                .add(rent)
                .addOnSuccessListener { documentReference ->
                    launch {
                        rent.id = documentReference.id
                        update(rent) // update with newly generated id
                    }
                }
                .addOnFailureListener { e ->
                    CommonUtils.printMessage("Not able to pay rent by ${rent.tName}: $e")
                }
        }
    }

    fun update(rent: Rent, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(rent.id)
            .set(rent)
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Rent by ${rent.tName} is updated")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to update rent paid by ${rent.tName}: $e")
            }
    }

    fun delete(rent: Rent, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(rent.id)
            .delete()
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Rent paid by ${rent.tName} is removed")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to remove rent paid by ${rent.tName}: $e")
            }
    }

    fun getById(id: String): Rent? {
        return getBy(Globals.gFieldId, id)
    }

    fun getByName(name: String) : Rent? {
        return getBy(Globals.gDefaultsName, name)
    }

    private fun getBy(fieldName: String, fieldValue: String): Rent? {
        val rents = getByTask(Connection.db.collection(collectionName).whereEqualTo(fieldName, fieldValue).get(Source.SERVER))
        return if (rents.isEmpty()) null else rents[0]
    }

    fun getAll(): ArrayList<Rent> {
        return getByTask(Connection.db.collection(collectionName).get()) // .orderBy(Globals.gFieldHouseName).get())
    }

    private fun getRentPaid(houseId: String, fromDate: Long, toDate: Long) : ArrayList<Rent> {
        val task = Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldHouseId, houseId)
            .whereGreaterThan("paidOn", fromDate)
            .whereLessThan("paidOn", toDate).get()
        return getByTask(task)
    }

    fun getTenants(houseId: String): ArrayList<Pair<String, String>> {
        try {
            val task = Connection.db.collection(collectionName)
                .whereEqualTo(Globals.gFieldHouseId, houseId)
                .whereNotEqualTo(Globals.gFieldTenantId, "")
//                .orderBy(Globals.gFieldTenantName)
//                .select("tenantId", "tenantName")
                .get()
            val result = Tasks.await(task)
            val list = result.mapTo(ArrayList()) { it.toObject(Rent::class.java) }
            return ArrayList(list.map { Pair(it.tId, it.tName) }) // it.toObject(Pair<String, String>)
        } catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    private fun getByTask(task: Task<QuerySnapshot>): ArrayList<Rent> {
        try {
            val result = Tasks.await(task) // https://stackoverflow.com/questions/55441428/how-to-wait-the-end-of-a-firebase-call-in-kotlin-android
            return result.mapTo(ArrayList()) {
                it.toObject(Rent::class.java)
            }
        }
        catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    fun getTotalRentsPaid(houseId: String, from: Long, to: Long): List<Pair<String, Int>> {
        return getRentPaid(houseId, from, to)
            .groupBy { it.hName } // Group by houseId
            .map { entry ->         // Map each group to a Pair<String, Int>
                entry.key to entry.value.sumOf { it.amount }
            }
    }

    fun getRentsPaidByTenantGrouped(houseId: String, from: Long, to: Long): List<Triple<String, String, Int>> {
        return getRentPaid(houseId, from, to)
            .groupBy { it.hName to it.tName }
            .map { entry ->
                val (houseName, tenantName) = entry.key
                Triple(houseName, tenantName, entry.value.sumOf { it.amount })
            }
    }

    fun getRentsPaidByTenant(houseId: String, tenantId: String, from: Long, to: Long): List<Rent> {
        val task = Connection.db.collection(collectionName).whereEqualTo(Globals.gFieldHouseId, houseId)
            .whereEqualTo(Globals.gFieldTenantId, tenantId)
            .whereGreaterThan("paidOn", from)
            .whereLessThan("paidOn", to).get()
        return getByTask(task)
    }

    fun getRentLatePayers(): List<Rent> {
        val task = Connection.db.collection(collectionName)
            .whereGreaterThan("delay", Globals.rentThreshold).get()
        return getByTask(task) // .map { Triple(it.tenantName, it.houseName, it.delay) }
    }

    fun getRentLatePayersByBuilding(buildingId: String): List<Rent> {
        val task = Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldBuildingId, buildingId)
            .whereGreaterThan("delay", Globals.rentThreshold).get()
        return getByTask(task) // .map { Triple(it.tenantName, it.houseName, it.delay) }
    }

    fun getRentLatePayersByHouse(houseId: String): List<Pair<String, Int>> {
        val task = Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldHouseId, houseId)
            .whereGreaterThan("delay", Globals.rentThreshold).get()
        return getByTask(task).map {
            Pair(it.tName, it.delay)
        }
    }

    fun getTenantDelayedPayments(tenantId: String): List<Pair<String, Int>> {
        val task = Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldTenantId, tenantId)
            .whereGreaterThan("delay", Globals.rentThreshold).get()
        return getByTask(task).map {
            Pair(it.hName, it.delay)
        }
    }

    fun getTenantDelayedPaymentsByHouse(tenantId: String, houseId: String): List<Int> {
        val task = Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldTenantId, tenantId)
            .whereEqualTo(Globals.gFieldHouseId, houseId)
            .whereGreaterThan("delay", Globals.rentThreshold).get()
        return getByTask(task).map { it.delay }
    }


    override fun processDocumentChange(documentChange: DocumentChange) { // this is not needed as we don't need to keep all rental payment in memory
        when (documentChange.type) {
            DocumentChange.Type.ADDED -> Rents.addLocally(documentChange.document.toObject(Rent::class.java))
            DocumentChange.Type.MODIFIED -> Rents.updateLocally(documentChange.document.toObject(Rent::class.java)) // this should not happen
            DocumentChange.Type.REMOVED -> Rents.deleteLocally(documentChange.document.toObject(Rent::class.java))
        }
    }
}