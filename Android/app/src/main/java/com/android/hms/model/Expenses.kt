package com.android.hms.model

import com.android.hms.db.ExpensesDb

data class Expense(var id: String = "", var category: String = "", var notes: String = "", var amount: Double = 0.0, var bId: String = "", var bName: String = "",
                   var hId: String = "", var hName: String = "", var paidType: String = "", var paidBy: String = "", var paidOn: Long = 0)

object Expenses {

    fun add(expense: Expense, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        ExpensesDb.add(expense) { success, error -> result(success, error) }
    }

    fun update(expense: Expense, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        ExpensesDb.update(expense) { success, error -> result(success, error) }
    }

    fun delete(expense: Expense, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        ExpensesDb.delete(expense) { success, error -> result(success, error) }
    }

    fun getByIdFromDb(id: String): Expense? {
        return ExpensesDb.getById(id)
    }

    fun getAllFromDb(): ArrayList<Expense> {
        return ExpensesDb.getAll()
    }

    fun getAllByBuilding(buildingId: String): ArrayList<Expense> {
        return ExpensesDb.getAllByBuilding(buildingId)
    }

    fun getAllByHouse(houseId: String): ArrayList<Expense> {
        return ExpensesDb.getAllByHouse(houseId)
    }
}