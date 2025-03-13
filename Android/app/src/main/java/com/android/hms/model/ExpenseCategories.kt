package com.android.hms.model

import com.android.hms.db.ExpenseCategoriesDb
import java.util.Collections

data class ExpenseCategory(var id: String = "", var name: String = "")

object ExpenseCategories {

    val categories: MutableList<String> = Collections.synchronizedList(ArrayList<String>())

    fun add(expenseCategory: ExpenseCategory, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        ExpenseCategoriesDb.add(expenseCategory) { success, error -> result(success, error) }
    }

    fun checkAndAdd(expenseCategory: ExpenseCategory) {
        if (categories.indexOf(expenseCategory.name) == -1) {
            categories.add(expenseCategory.name)
            add(expenseCategory)
        }
    }

    fun update(expenseCategory: ExpenseCategory, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        ExpenseCategoriesDb.update(expenseCategory) { success, error -> result(success, error) }
    }

    fun delete(expenseCategory: ExpenseCategory, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        ExpenseCategoriesDb.delete(expenseCategory) { success, error -> result(success, error) }
    }

    fun getById(id: String): ExpenseCategory? {
        return ExpenseCategoriesDb.getById(id)
    }

    fun getByName(name: String): ExpenseCategory? {
        return ExpenseCategoriesDb.getByName(name)
    }

    fun getAll(): ArrayList<ExpenseCategory> {
        return ExpenseCategoriesDb.getAll()
    }

    fun refreshList() {
        categories.clear()
        ExpenseCategoriesDb.getAll().forEach { categories.add(it.name) }
        val duplicates = categories.groupBy { it }
            .filter { it.value.size > 1 }
            .keys
        duplicates.forEach { delete(getByName(it) ?: return@forEach) }
    }

    fun addLocally(expenseCategory: ExpenseCategory) {
        if (expenseCategory.id.isEmpty()) return
        if (categories.indexOf(expenseCategory.name) == -1) categories.add(expenseCategory.name)
    }

    fun updateLocally(expenseCategory: ExpenseCategory) {
        if (expenseCategory.id.isEmpty()) return
        addLocally(expenseCategory)
        // this should not happen
    }

    fun deleteLocally(expenseCategory: ExpenseCategory) {
        if (expenseCategory.id.isEmpty()) return
        if (categories.indexOf(expenseCategory.name) != -1) categories.remove(expenseCategory.name)
    }
}