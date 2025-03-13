package com.android.hms.db

import android.util.Log
import com.android.hms.model.User
import com.android.hms.model.Users
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

object UsersDb : DbBase() {
    private const val TAG = "UsersDB"
    override val collectionName = if (Globals.gDevelopmentMode) "dev_users" else "users"

    fun add(user: User, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .add(user)
            .addOnSuccessListener { documentReference ->
                launch {
                    user.id = documentReference.id
                    update(user) // update with newly generated id
                    result(true, "")
                    CommonUtils.printMessage("User ${user.name}(${user.id}) is added")
                }
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to add user ${user.name}: $e")
            }
    }

    fun add(users: ArrayList<User>) {
        for (user in users) {
            Connection.db.collection(collectionName)
                .add(user)
                .addOnSuccessListener { documentReference ->
                    launch {
                        user.id = documentReference.id
                        update(user) // update with newly generated id
                    }
                }
                .addOnFailureListener { e ->
                    CommonUtils.printMessage("Not able to add user ${user.name}: $e")
                }
        }
    }

    fun update(user: User, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(user.id)
            .set(user)
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("User ${user.name} is updated")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to update user ${user.name}: $e")
            }
    }

    fun delete(user: User, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        delete(user.id, user.name, result)
    }

    fun delete(id: String, name: String, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(id)
            .delete()
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("User $name is removed")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to remove user $name: $e")
            }
    }

    fun get(name: String, phone: String): User? {
        val users = getByTask(Connection.db.collection(collectionName).whereEqualTo(Globals.gDefaultsName, name).whereEqualTo(Globals.gUserDefaultsPhone, phone).get())
        return if (users.isEmpty()) null else users[0]
    }

    fun getById(id: String): User? {
        return getBy(Globals.gFieldId, id)
    }

    fun getByPhone(phone: String): User? {
        return getBy(Globals.gUserDefaultsPhone, phone)
    }

    fun getByName(name: String): User? {
        return getBy(Globals.gDefaultsName, name)
    }

    fun getByEmail(email: String): User? {
        return getBy(Globals.gUserDefaultsEmail, email)
    }

    fun getByToken(token: String): User? {
        return getBy(Globals.gUserDefaultsToken, token)
    }

    private fun getBy(fieldName: String, fieldValue: String): User? {
        val users = getByTask(Connection.db.collection(collectionName).whereEqualTo(fieldName, fieldValue).get(Source.SERVER))
        return if (users.isEmpty()) null else users[0]
    }

    fun getAll(): ArrayList<User> {
        return getByTask(Connection.db.collection(collectionName).get())
    }

    fun getHelpers(): ArrayList<User> {
        return getByTask(
            Connection.db.collection(collectionName).whereEqualTo("role", Users.Roles.HELPER.value)
//                .orderBy(Globals.gDefaultsName)
                .get()
        )
    }

    fun getTenants(): ArrayList<User> {
        return getByTask(
            Connection.db.collection(collectionName).whereEqualTo("role", Users.Roles.TENANT.value)
  //              .orderBy(Globals.gDefaultsName)
                .get()
        )
    }

    private fun getByTask(task: Task<QuerySnapshot>): ArrayList<User> {
        try {
            val result = Tasks.await(task) // https://stackoverflow.com/questions/55441428/how-to-wait-the-end-of-a-firebase-call-in-kotlin-android
            return result.mapTo(ArrayList()) {
                it.toObject(User::class.java)
            }
        } catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    override fun processDocumentChange(documentChange: DocumentChange) {
        when (documentChange.type) {
            DocumentChange.Type.ADDED -> Users.addLocally(documentChange.document.toObject(User::class.java))
            DocumentChange.Type.MODIFIED -> Users.updateLocally(documentChange.document.toObject(User::class.java))
            DocumentChange.Type.REMOVED -> Users.deleteLocally(documentChange.document.toObject(User::class.java))
        }
    }

    fun updateEmailId(userId: String, emailId: String) {
        val userRef = Connection.db.collection(collectionName).document(userId)
        val updatedEmailId = mapOf("emailId" to emailId)
        userRef.update(updatedEmailId)
            .addOnSuccessListener {
                // Success - field updated
                Log.d(TAG, "User email ID updated successfully")
            }
            .addOnFailureListener { exception ->
                // Failure - handle error
                Log.w(TAG, "Error updating user email ID", exception)
            }

        /* sample code
        val updatedFields = mapOf(
            "role" to "admin",
            "email" to "new.email@example.com"
        )
        userRef.update(updatedFields) */
    }
}