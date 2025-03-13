package com.android.hms.db

import com.android.hms.model.ExpenseCategories
import com.android.hms.model.ExpenseCategory
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

object ExpenseCategoriesDb : DbBase() {
    private const val TAG = "ExpenseCategoriesDB"
    override val collectionName = if (Globals.gDevelopmentMode) "dev_expense_categories" else "expense_categories"

    fun add(expenseCategory: ExpenseCategory, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .add(expenseCategory)
            .addOnSuccessListener { documentReference ->
                launch {
                    expenseCategory.id = documentReference.id
                    update(expenseCategory) // update with newly generated id
                    result(true, "")
                    CommonUtils.printMessage("Category ${expenseCategory.name}(${expenseCategory.id}) is added")
                }
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to add category ${expenseCategory.name}: $e")
            }
    }

    fun update(expenseCategory: ExpenseCategory, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(expenseCategory.id)
            .set(expenseCategory)
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Category ${expenseCategory.name} is updated")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to update category ${expenseCategory.name}: $e")
            }
    }

    fun delete(expenseCategory: ExpenseCategory, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        val categoryRef = Connection.db.collection(collectionName).document(expenseCategory.id)
        categoryRef.delete()
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Category ${expenseCategory.name} is removed")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to remove category ${expenseCategory.name}: $e")
            }
    }

    fun getById(id: String): ExpenseCategory? {
        return getBy(Globals.gFieldId, id)
    }

    fun getByName(name: String): ExpenseCategory? {
        return getBy(Globals.gDefaultsName, name)
    }

    private fun getBy(fieldName: String, fieldValue: String): ExpenseCategory? {
        val categories = getByTask(Connection.db.collection(collectionName).whereEqualTo(fieldName, fieldValue).get(Source.SERVER))
        return if (categories.isEmpty()) null else categories[0]
    }

    fun getAll(): ArrayList<ExpenseCategory> {
        return getByTask(Connection.db.collection(collectionName).get())
    }

    private fun getByTask(task: Task<QuerySnapshot>): ArrayList<ExpenseCategory> {
        try {
            val result = Tasks.await(task) // https://stackoverflow.com/questions/55441428/how-to-wait-the-end-of-a-firebase-call-in-kotlin-android
            return result.mapTo(ArrayList()) {
                it.toObject(ExpenseCategory::class.java)
            }
        } catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    override fun processDocumentChange(documentChange: DocumentChange) {
        when (documentChange.type) {
            DocumentChange.Type.ADDED -> ExpenseCategories.addLocally(documentChange.document.toObject(ExpenseCategory::class.java))
            DocumentChange.Type.MODIFIED -> ExpenseCategories.updateLocally(documentChange.document.toObject(ExpenseCategory::class.java))
            DocumentChange.Type.REMOVED -> ExpenseCategories.deleteLocally(documentChange.document.toObject(ExpenseCategory::class.java))
        }
    }
}