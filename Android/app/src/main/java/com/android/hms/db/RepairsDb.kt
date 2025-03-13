package com.android.hms.db

import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

object RepairsDb : DbBase() {
    private const val TAG = "RepairsDB"
    override val collectionName = if (Globals.gDevelopmentMode) "dev_repairs" else "repairs"

    fun add(repair: Repair, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .add(repair)
            .addOnSuccessListener { documentReference ->
                launch {
                    repair.id = documentReference.id
                    update(repair) // update with newly generated id
                    result(true, "")
                    CommonUtils.printMessage("Repair ${repair.desc}(${repair.id}) is added")
                }
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to add repair ${repair.desc}: $e")
            }
    }

    fun add(repairs: ArrayList<Repair>) {
        for (repair in repairs) {
            Connection.db.collection(collectionName)
                .add(repair)
                .addOnSuccessListener { documentReference ->
                    launch {
                        repair.id = documentReference.id
                        update(repair) // update with newly generated id
                    }
                }
                .addOnFailureListener { e ->
                    CommonUtils.printMessage("Not able to add repair ${repair.desc}: $e")
                }
        }
    }

    fun update(repair: Repair, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(repair.id)
            .set(repair)
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Repair ${repair.desc} is updated")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to update repair ${repair.desc}: $e")
            }
    }

    fun delete(repair: Repair, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(repair.id)
            .delete()
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Repair ${repair.desc} is removed")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to remove repair ${repair.desc}: $e")
            }
    }

    fun getById(id: String): Repair? {
        return getBy(Globals.gFieldId, id)
    }

    private fun getBy(fieldName: String, fieldValue: String): Repair? {
        val repairs = getByTask(Connection.db.collection(collectionName).whereEqualTo(fieldName, fieldValue).get(Source.SERVER))
        return if (repairs.isEmpty()) null else repairs[0]
    }

    fun getAllFixedToday(): ArrayList<Repair> {
        return getAllByGreaterThan("fixedOn", CommonUtils.getMillisecondsTillYesterday())
    }

    fun getAllPaidToday(): ArrayList<Repair> {
        return getAllByGreaterThan("paidOn",  CommonUtils.getMillisecondsTillYesterday())
    }

    fun getAllNotFixed(): ArrayList<Repair> {
        return getAllByEquals0("fixedOn")
    }

    fun getAllNotPaid(): ArrayList<Repair> {
        return getAllByEquals0("paidOn")
    }

    fun getAllNew(): ArrayList<Repair> {
        return getAllBy("status", Repairs.statuses.first())
    }

    fun getAllNotClosed(): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereNotEqualTo("status", Repairs.statuses.last()).get())
    }

    fun getAllByTenantId(tenantId: String): ArrayList<Repair> {
        return getAllBy(Globals.gFieldTenantId, tenantId)
    }

    fun getAllByBuilding(buildingId: String): ArrayList<Repair> {
        return getAllBy(Globals.gFieldBuildingId, buildingId)
    }

    fun getAllByHouse(houseId: String): ArrayList<Repair> {
        return getAllBy(Globals.gFieldHouseId, houseId)
    }


    fun getAllOpenOverWeek(): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereLessThan("raisedOn", CommonUtils.getLastWeekTime())
            .whereEqualTo("fixedOn", 0)
            .get())
    }

    fun getAllNotPaidOverWeek(): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereLessThan("fixedOn", CommonUtils.getLastWeekTime())
            .whereEqualTo("paidOn", 0)
            .get())
    }

    fun getTenantPaid(isTenantPaid: Boolean): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo("tPaid", isTenantPaid).get())
    }

    fun getAllOpenByBuilding(buildingId: String): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldBuildingId, buildingId)
            .whereEqualTo("fixedOn", 0)
            .get())
    }

    fun getAllOpenByHouse(houseId: String): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldHouseId, houseId)
            .whereEqualTo("fixedOn", 0)
            .get())
    }

    private fun getAllBy(fieldName: String, fieldValue: String): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo(fieldName, fieldValue).get())
    }

    private fun getAllByEquals0(fieldName: String): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo(fieldName, 0).get())
    }

    private fun getAllByGreaterThan(fieldName: String, fieldValue: Long): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName)
            .whereGreaterThan(fieldName, fieldValue).get())
    }

    fun getAll(): ArrayList<Repair> {
        return getByTask(Connection.db.collection(collectionName).get())
    }

    private fun getByTask(task: Task<QuerySnapshot>) : ArrayList<Repair> {
        try {
            val result = Tasks.await(task) // https://stackoverflow.com/questions/55441428/how-to-wait-the-end-of-a-firebase-call-in-kotlin-android
            return result.mapTo(ArrayList()) {
                it.toObject(Repair::class.java)
            }
        }
        catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    override fun processDocumentChange(documentChange: DocumentChange) {
        val repair = documentChange.document.toObject(Repair::class.java)
        when (documentChange.type) {
            DocumentChange.Type.ADDED -> if (repair.status != Repairs.statuses.last()) Repairs.addLocally(repair)
            DocumentChange.Type.MODIFIED -> if (repair.status != Repairs.statuses.last()) Repairs.updateLocally(repair)
            DocumentChange.Type.REMOVED -> Repairs.deleteLocally(repair)
        }
        if (repair.status == Repairs.statuses.last()) Repairs.deleteLocally(repair)
    }
}