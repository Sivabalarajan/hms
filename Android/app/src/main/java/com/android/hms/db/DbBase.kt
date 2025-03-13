package com.android.hms.db

import com.android.hms.utils.CommonUtils
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class DbBase : CoroutineScope {
    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + Job()
    protected abstract val collectionName: String
    protected abstract fun processDocumentChange(documentChange: DocumentChange)

    open fun observeCollectionChanges() {
        val query = Connection.db.collection(collectionName)
        query.addSnapshotListener { snapshot, exception ->
            launch {
                if (exception != null) {
                    CommonUtils.printMessage("Listener failed: $exception")
                    return@launch
                }
                val documentSnapshot = snapshot ?: return@launch
                if (documentSnapshot.metadata.isFromCache) return@launch
                // if (documentSnapshot.metadata.isFromCache || documentSnapshot.metadata.hasPendingWrites()) return@launch
                for (dc in documentSnapshot.documentChanges) processDocumentChange(dc)
            }
        }
    }
}