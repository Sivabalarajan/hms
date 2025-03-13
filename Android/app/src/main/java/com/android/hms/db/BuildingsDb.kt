package com.android.hms.db

import com.android.hms.model.Building
import com.android.hms.model.Buildings
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
import kotlin.coroutines.CoroutineContext

object BuildingsDb : DbBase() {
    private const val TAG = "BuildingsDB"
    override val collectionName = if (Globals.gDevelopmentMode) "dev_buildings" else "buildings"

    fun add(building: Building, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .add(building)
            .addOnSuccessListener { documentReference ->
                launch {
                    building.id = documentReference.id
                    update(building) // update with newly generated id
                    result(true, "")
                    CommonUtils.printMessage("Building ${building.name}(${building.id}) is added")
                }
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to add building ${building.name}: $e")
            }
    }

    fun add(buildings: ArrayList<Building>) {
        for (building in buildings) {
            Connection.db.collection(collectionName)
                .add(building)
                .addOnSuccessListener { documentReference ->
                    launch {
                        building.id = documentReference.id
                        update(building) // update with newly generated id
                    }
                }
                .addOnFailureListener { e ->
                    CommonUtils.printMessage("Not able to add building ${building.name}: $e")
                }
        }
    }

    fun update(building: Building, result:(success: Boolean, error: String) -> Unit= { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(building.id)
            .set(building)
            .addOnSuccessListener {
                result(true,"")
                CommonUtils.printMessage("Building ${building.name} is updated")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to update building ${building.name}: $e")
            }
    }

    fun delete(building: Building, result:(success: Boolean, error: String) -> Unit= { _, _ -> }) {
        val buildingRef = Connection.db.collection(collectionName).document(building.id)
        buildingRef.delete()
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Building ${building.name} is removed")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to remove building ${building.name}: $e")
            }
    }

    fun getById(id: String): Building? {
        return getBy(Globals.gFieldId, id)
    }

    fun getByName(name: String) : Building? {
        return getBy(Globals.gDefaultsName, name)
    }

    private fun getBy(fieldName: String, fieldValue: String): Building? {
        val buildings = getByTask(Connection.db.collection(collectionName).whereEqualTo(fieldName, fieldValue).get(Source.SERVER))
        return if (buildings.isEmpty()) null else buildings[0]
    }

    fun getAll(): ArrayList<Building> {
        return getByTask(Connection.db.collection(collectionName).get())
    }

    private fun getByTask(task: Task<QuerySnapshot>) : ArrayList<Building> {
        try {
            val result = Tasks.await(task) // https://stackoverflow.com/questions/55441428/how-to-wait-the-end-of-a-firebase-call-in-kotlin-android
            return result.mapTo(ArrayList()) {
                it.toObject(Building::class.java)
            }
        }
        catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    override fun processDocumentChange(documentChange: DocumentChange) {
        when (documentChange.type) {
            DocumentChange.Type.ADDED -> Buildings.addLocally(documentChange.document.toObject(Building::class.java))
            DocumentChange.Type.MODIFIED -> Buildings.updateLocally(documentChange.document.toObject(Building::class.java))
            DocumentChange.Type.REMOVED -> Buildings.deleteLocally(documentChange.document.toObject(Building::class.java))
        }
    }
}