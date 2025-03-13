package com.android.hms.db

import com.android.hms.model.RepairDescriptions
import com.android.hms.model.RepairDescription
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

object RepairDescriptionsDb : DbBase() {
    private const val TAG = "RepairDescriptionDb"
    override val collectionName = if (Globals.gDevelopmentMode) "dev_repair_descriptions" else "repair_descriptions"

    fun add(repairDescription: RepairDescription, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .add(repairDescription)
            .addOnSuccessListener { documentReference ->
                launch {
                    repairDescription.id = documentReference.id
                    update(repairDescription) // update with newly generated id
                    result(true, "")
                    CommonUtils.printMessage("Description ${repairDescription.desc}(${repairDescription.id}) is added")
                }
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to add description ${repairDescription.desc}: $e")
            }
    }

    fun update(repairDescription: RepairDescription, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(repairDescription.id)
            .set(repairDescription)
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Description ${repairDescription.desc} is updated")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to update description ${repairDescription.desc}: $e")
            }
    }

    fun delete(repairDescription: RepairDescription, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        val descriptionRef = Connection.db.collection(collectionName).document(repairDescription.id)
        descriptionRef.delete()
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Description ${repairDescription.desc} is removed")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to remove description ${repairDescription.desc}: $e")
            }
    }

    fun getById(id: String): RepairDescription? {
        return getBy(Globals.gFieldId, id)
    }

    fun getByName(name: String): RepairDescription? {
        return getBy(Globals.gDefaultsName, name)
    }

    private fun getBy(fieldName: String, fieldValue: String): RepairDescription? {
        val categories = getByTask(Connection.db.collection(collectionName).whereEqualTo(fieldName, fieldValue).get(Source.SERVER))
        return if (categories.isEmpty()) null else categories[0]
    }

    fun getAll(): ArrayList<RepairDescription> {
        return getByTask(Connection.db.collection(collectionName).get())
    }

    private fun getByTask(task: Task<QuerySnapshot>): ArrayList<RepairDescription> {
        try {
            val result = Tasks.await(task) // https://stackoverflow.com/questions/55441428/how-to-wait-the-end-of-a-firebase-call-in-kotlin-android
            return result.mapTo(ArrayList()) {
                it.toObject(RepairDescription::class.java)
            }
        } catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    override fun processDocumentChange(documentChange: DocumentChange) {
        when (documentChange.type) {
            DocumentChange.Type.ADDED -> RepairDescriptions.addLocally(documentChange.document.toObject(RepairDescription::class.java))
            DocumentChange.Type.MODIFIED -> RepairDescriptions.updateLocally(documentChange.document.toObject(RepairDescription::class.java))
            DocumentChange.Type.REMOVED -> RepairDescriptions.deleteLocally(documentChange.document.toObject(RepairDescription::class.java))
        }
    }
}