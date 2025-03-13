package com.android.hms.db

import com.android.hms.model.Expense
import com.android.hms.model.Expenses
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.android.hms.viewmodel.SharedViewModelSingleton
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot

object ExpensesDb : DbBase() {
    private const val TAG = "ExpensesDB"
    override val collectionName = if (Globals.gDevelopmentMode) "dev_expenses" else "expenses"

    fun add(expense: Expense, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .add(expense)
            .addOnSuccessListener { documentReference ->
                launch {
                    expense.id = documentReference.id
                    update(expense) // update with newly generated id
                    result(true, "")
                    CommonUtils.printMessage("Expense ${expense.category}(${expense.id}) is added")
                }
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to add expense ${expense.category}: $e")
            }
    }

    fun add(expenses: ArrayList<Expense>) {
        for (expense in expenses) {
            Connection.db.collection(collectionName)
                .add(expense)
                .addOnSuccessListener { documentReference ->
                    launch {
                        expense.id = documentReference.id
                        update(expense) // update with newly generated id
                    }
                }
                .addOnFailureListener { e ->
                    CommonUtils.printMessage("Not able to add expense ${expense.category}: $e")
                }
        }
    }

    fun update(expense: Expense, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(expense.id)
            .set(expense)
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Expense ${expense.category} is updated")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to update expense ${expense.category}: $e")
            }
    }

    fun delete(expense: Expense, result:(success: Boolean, error: String) -> Unit = { _, _ -> }) {
        Connection.db.collection(collectionName)
            .document(expense.id)
            .delete()
            .addOnSuccessListener {
                result(true, "")
                CommonUtils.printMessage("Expense ${expense.category} is removed")
            }
            .addOnFailureListener { e ->
                result(false, e.toString())
                CommonUtils.printMessage("Not able to remove expense ${expense.category}: $e")
            }
    }

    fun getById(id: String): Expense? {
        return getBy(Globals.gFieldId, id)
    }

    private fun getBy(fieldName: String, fieldValue: String): Expense? {
        val expenses = getByTask(Connection.db.collection(collectionName).whereEqualTo(fieldName, fieldValue).get(Source.SERVER))
        return if (expenses.isEmpty()) null else expenses[0]
    }

    fun getAllByBuilding(buildingId: String): ArrayList<Expense> {
        return getAllBy(Globals.gFieldBuildingId, buildingId)
    }

    fun getAllByHouse(houseId: String): ArrayList<Expense> {
        return getAllBy(Globals.gFieldHouseId, houseId)
    }

    fun getAllCategories(): ArrayList<String> {
        return getAll()
            .groupBy { it.category }
            .map { entry -> entry.key }
            .sortedBy { it }
            .toCollection(ArrayList())
    }

    fun getAllByCategory(category: String): ArrayList<Expense> {
        return getAllBy("category", category)
    }

    private fun getAllBy(fieldName: String, fieldValue: String): ArrayList<Expense> {
        return getByTask(Connection.db.collection(collectionName)
            .whereEqualTo(fieldName, fieldValue).get())
    }

    fun getAll(): ArrayList<Expense> {
        return getByTask(Connection.db.collection(collectionName).get())
    }

    private fun getByTask(task: Task<QuerySnapshot>) : ArrayList<Expense> {
        try {
            val result = Tasks.await(task) // https://stackoverflow.com/questions/55441428/how-to-wait-the-end-of-a-firebase-call-in-kotlin-android
            return result.mapTo(ArrayList()) {
                it.toObject(Expense::class.java)
            }
        }
        catch (e: Exception) {
            CommonUtils.printException(e)
        }
        return ArrayList()
    }

    override fun processDocumentChange(documentChange: DocumentChange) {
        val expense = documentChange.document.toObject(Expense::class.java)
        when (documentChange.type) {
            DocumentChange.Type.ADDED -> SharedViewModelSingleton.expenseSubmittedEvent.postValue(expense)
            DocumentChange.Type.MODIFIED -> SharedViewModelSingleton.expenseUpdatedEvent.postValue(expense)
            DocumentChange.Type.REMOVED -> SharedViewModelSingleton.expenseRemovedEvent.postValue(expense)
        }
    }
}