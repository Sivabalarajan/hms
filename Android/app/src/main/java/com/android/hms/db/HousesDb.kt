package com.android.hms.db

import com.android.hms.model.House
import com.android.hms.model.Houses
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

object HousesDb : DbBase() {
    private const val TAG = "HousesDB"
    override val collectionName = if (Globals.gDevelopmentMode) "dev_houses" else "houses"

    fun add(house: House, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .add(house)
            .addOnSuccessListener { documentReference ->
                launch {
                    house.id = documentReference.id
                    update(house) // update with newly generated id
                    result(true, "")
                    CommonUtils.printMessage("House ${house.name}(${house.id}) is added")
                }
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to add house ${house.name}: $e")
            }
    }

    fun add(houses: ArrayList<House>) {
        for (house in houses) {
            Connection.db.collection(collectionName)
                .add(house)
                .addOnSuccessListener { documentReference ->
                    launch {
                        house.id = documentReference.id
                        update(house) // update with newly generated id
                    }
                }
                .addOnFailureListener { e ->
                    CommonUtils.printMessage("Not able to add house ${house.name}: $e")
                }
        }
    }

    fun update(house: House, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(house.id)
            .set(house)
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("House ${house.name} is updated")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to update house ${house.name}: $e")
            }
    }

    fun delete(house: House, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(house.id)
            .delete()
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("House ${house.name} is removed")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to remove house ${house.name}: $e")
            }
    }

    fun deleteByBuilding(buildingId: String) {
        Connection.db.collection(collectionName).whereEqualTo(Globals.gFieldBuildingId, buildingId).get()
            .addOnSuccessListener { housesSS ->
                val houseDList = housesSS.chunked(499) // Split the list into chunks of 500 because of Firestore limit
                houseDList.forEachIndexed { index, houseDChunk ->
                    val batch = Connection.db.batch() // Perform operations on each chunk
                    for (houseD in houseDChunk) batch.delete(houseD.reference)
                    batch.commit() // Commit the batch
                        .addOnSuccessListener { } // deletion is successful
                        .addOnFailureListener { exp ->
                            CommonUtils.printMessage("House batch $index is not deleted. Error is ${exp.message}")
                        }
                }
                /* for(houseD in housesSS) houseD.reference.delete()
                    .addOnSuccessListener { }
                    .addOnFailureListener { exp ->
                        CommonUtils.printMessage("House ${houseD.reference.id} is not deleted. Error is ${exp.message}")
                    } */
            }
            .addOnFailureListener { e ->
                CommonUtils.printMessage("Houses associated with building $buildingId are not deleted. Error is ${e.message}")
            }
    }

    fun getById(id: String): House? {
        return getBy(Globals.gFieldId, id)
    }

    fun getByName(name: String): House? {
        return getBy(Globals.gDefaultsName, name)
    }

    fun getByTenant(tenantId: String) : ArrayList<House> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldTenantId, tenantId).get())
    }

    fun getTenants(): ArrayList<House> {
        return getByTask(Connection.db.collection(collectionName)
            .whereNotEqualTo(Globals.gFieldTenantId, "").get())
            // .orderBy(Globals.gFieldTenantName).get())
    }

    private fun getBy(fieldName: String, fieldValue: String): House? {
        val houses = getByTask(Connection.db.collection(collectionName).whereEqualTo(fieldName, fieldValue).get(Source.SERVER))
        return if (houses.isEmpty()) null else houses[0]
    }

    fun getAllBy(buildingId: String): ArrayList<House> {
        return getByTask(Connection.db.collection(collectionName).whereEqualTo(Globals.gFieldBuildingId, buildingId).get())
            // .orderBy(Globals.gFieldBuildingName).orderBy(Globals.gDefaultsName).get())
    }

    fun getAllByBuildingName(buildingName: String): ArrayList<House> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldBuildingName, buildingName).get())
            // .orderBy(Globals.gDefaultsName).get())
    }

    fun getAllVacant(): ArrayList<House> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo(Globals.gFieldTenantId, "").get())
            // .orderBy(Globals.gFieldBuildingName).orderBy(Globals.gDefaultsName).get())
    }

    fun getRentNotPaidTenants(): ArrayList<House> {
        return getByTask(Connection.db.collection(collectionName)
            .whereNotEqualTo(Globals.gFieldTenantId, "")
            .whereLessThan("rPaid", CommonUtils.getLastMonthTime()).get())
            // .orderBy(Globals.gFieldBuildingName).orderBy(Globals.gDefaultsName).get())
    }

    fun getAll(): ArrayList<House> {
        return getByTask(Connection.db.collection(collectionName).get())
            // .orderBy(Globals.gFieldBuildingName).orderBy(Globals.gDefaultsName).get())
    }

    private fun getByTask(task: Task<QuerySnapshot>) : ArrayList<House> {
        try {
            val result = Tasks.await(task) // https://stackoverflow.com/questions/55441428/how-to-wait-the-end-of-a-firebase-call-in-kotlin-android
            return result.mapTo(ArrayList()) {
                it.toObject(House::class.java)
            }
        }
        catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    override fun processDocumentChange(documentChange: DocumentChange) {
        when (documentChange.type) {
            DocumentChange.Type.ADDED -> Houses.addLocally(documentChange.document.toObject(House::class.java))
            DocumentChange.Type.MODIFIED -> Houses.updateLocally(documentChange.document.toObject(House::class.java))
            DocumentChange.Type.REMOVED -> Houses.deleteLocally(documentChange.document.toObject(House::class.java))
        }
    }
}