package com.android.hms.db

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

object Connection : CoroutineScope {

    val db get() = FirebaseFirestore.getInstance() // Initialize  / get Firestore

    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + Job()

    @Synchronized
    fun setObservers() {
        UsersDb.observeCollectionChanges()
        BuildingsDb.observeCollectionChanges()
        HousesDb.observeCollectionChanges()
        RepairsDb.observeCollectionChanges()
        ExpenseCategoriesDb.observeCollectionChanges()
    }

    fun getData() {
        // Reference to a specific document
        db.collection("users").document("user_id_here")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document: DocumentSnapshot? = task.result
                    if (document != null && document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    } else {
                        Log.d(TAG, "No such document")
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.exception)
                }
            }
    }

    fun listenForUpdates() {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document("user_id_here")
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d(TAG, "Current data: ${documentSnapshot.data}")
                } else {
                    Log.d(TAG, "Current data: null")
                }
            }
    }

    fun queryData() {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereGreaterThan("born", 1900)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(TAG, "Document data: ${document.data}")
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting documents: ", e)
            }
    }

    /* fun batchWrite() {
        val db = FirebaseFirestore.getInstance()

        // Create a new batch
        val batch: WriteBatch = db.batch()

        // Create new user document 1
        val user1 = db.collection("users").document("user1_id")
        batch.set(user1, hashMapOf("first" to "Grace", "last" to "Hopper", "born" to 1906))

        // Create new user document 2
        val user2 = db.collection("users").document("user2_id")
        batch.set(user2, hashMapOf("first" to "Alan", "last" to "Turing", "born" to 1912))

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Batch write successful!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error writing batch", e)
            }
    } */


    private const val TAG = "DB.Connection"
}