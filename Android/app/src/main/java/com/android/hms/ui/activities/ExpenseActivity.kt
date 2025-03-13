package com.android.hms.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.ExpenseCategories
import com.android.hms.model.ExpenseCategory
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.model.Expense
import com.android.hms.model.Expenses
import com.android.hms.ui.ExpenseActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseActivity: BaseActivity() {

    private var buildingId = ""
    private var buildingName = ""
    private var expenseId = ""
    private var expense: Expense? = null
    private var house: House? = null

    private lateinit var tvExpenseCategory: AutoCompleteTextView
    private lateinit var etExpenseNotes: EditText
    private lateinit var etExpenseAmount: EditText
    private lateinit var etPaymentType: EditText
    private lateinit var etPaidBy: EditText
    private lateinit var etPaidDate: EditText
    private lateinit var btnSubmitExpense: Button
    private lateinit var btnRemoveExpense: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense)
        setActionBarView("Building Expense")

        expenseId = intent.getStringExtra(Globals.gFieldId) ?: ""

        buildingId = intent.getStringExtra("buildingId") ?: ""
        buildingName = intent.getStringExtra("buildingName") ?: ""
        if (buildingId.isEmpty() || buildingName.isEmpty()) {
            CommonUtils.showMessage(context, "Building", "Not able to get the building details. Please try again later.")
            return
        }

        val progressBar = MyProgressBar(context)

        val houseId = intent.getStringExtra("houseId") ?: ""
        if (houseId.isNotEmpty()) {
            house = Houses.getById(houseId) ?: return
            buildingId = house?.bId ?: return
            buildingName = house?.bName ?: return
            setActionBarView("House ${house?.name} Expense")
            findViewById<TextView>(R.id.etBuildingHouseDetails).text = if (expenseId.isEmpty()) "Submit expense details on house ${house?.name} in ${house?.bName}"
                else "Update expense details on house ${house?.name} in ${house?.bName}"
        }
        else {
            setActionBarView("Building $buildingName Expense")
            findViewById<TextView>(R.id.etBuildingHouseDetails).text = if (expenseId.isEmpty()) "Submit expense details on building $buildingName"
                else "Update expense details on building $buildingName"
        }

        initObjects()
        progressBar.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { return true }

    private fun initObjects() {
        val progressBar = MyProgressBar(context)
        tvExpenseCategory = findViewById(R.id.tvExpenseCategory)
        etExpenseNotes = findViewById(R.id.etExpenseNotes)
        etExpenseAmount = findViewById(R.id.etExpenseAmount)
        etPaymentType = findViewById(R.id.etPaymentType)
        etPaidDate = findViewById(R.id.etPaidDate)
        etPaidBy = findViewById(R.id.etPaidBy)
        btnSubmitExpense = findViewById(R.id.btnSubmitExpense)
        btnRemoveExpense = findViewById(R.id.btnRemoveExpense)
        CommonUtils.pickAndSetDate(etPaidDate)
        CommonUtils.setDate(etPaidDate, CommonUtils.currentTime) // default

        val categories = ExpenseCategories.categories.sorted()
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
        tvExpenseCategory.setAdapter(categoryAdapter)
        tvExpenseCategory.setOnClickListener { tvExpenseCategory.showDropDown() }
        tvExpenseCategory.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) tvExpenseCategory.showDropDown() }

        btnSubmitExpense.setOnClickListener { if (expenseId.isEmpty()) initiateNewExpense() else updateExpense() }
        btnRemoveExpense.setOnClickListener { removeExpense() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }

        if (expenseId.isEmpty()) {
            btnRemoveExpense.visibility = View.GONE
            progressBar.dismiss()
        }
        else {
            expense = SharedViewModelSingleton.currentExpenseObject
            SharedViewModelSingleton.currentExpenseObject = null
            lifecycleScope.launch {
            if (expense == null) expense = withContext(Dispatchers.IO) { Expenses.getByIdFromDb(expenseId) }
                btnSubmitExpense.text = "Update Expense"
                tvExpenseCategory.setText(expense?.category)
                etExpenseNotes.setText(expense?.notes)
                etExpenseAmount.setText("${expense?.amount}")
                etPaymentType.setText(expense?.paidType)
                etPaidBy.setText(expense?.paidBy)
                CommonUtils.setDate(etPaidDate, expense?.paidOn ?: return@launch)
                progressBar.dismiss()
            }
        }
    }

    private fun validateEntries() : Boolean {
        val errorMessage = StringBuilder()
        val currentTime = CommonUtils.currentTime
        val paidDate = etPaidDate.tag as Long
        val expenseAmount = etExpenseAmount.text.toString().trim().toDoubleOrNull()
        if (expenseAmount == null || expenseAmount < 0) errorMessage.append("Please enter valid expense amount.\n")
        if (tvExpenseCategory.text.toString().trim().isEmpty()) errorMessage.append("Please enter or choose valid expense category.\n")
        if (paidDate == 0L || paidDate > currentTime ) errorMessage.append("Please select a valid paid date.\n")
        if (expenseAmount == 0.0) errorMessage.append("Please enter valid expense amount.\n")

        if (errorMessage.isNotEmpty()) {
            CommonUtils.showMessage(context, "Invalid entry / entries", errorMessage.toString())
            return false
        }

        return true
    }

    private fun prepareExpenseObjectFromEnteredValues(): Expense {
        val expenseAmount = etExpenseAmount.text.toString().trim().toDouble()
        val category = tvExpenseCategory.text.toString().trim()
        val notes = etExpenseNotes.text.toString().trim()
        val paymentType = etPaymentType.text.toString().trim()
        val paidBy = etPaidBy.text.toString().trim()
        val paidOn = etPaidDate.tag as Long

        val expense = expense ?: return Expense(category = category, notes = notes, bId = buildingId, bName = buildingName, hId = house?.id ?: "", hName = house?.name ?: "",
            paidOn = paidOn, paidBy = paidBy, amount = expenseAmount, paidType = paymentType)

        expense.category = category
        expense.notes = notes
        expense.paidType = paymentType
        expense.paidOn = paidOn
        expense.paidBy = paidBy
        expense.amount = expenseAmount

        return expense
    }

    private fun initiateNewExpense() {
        if (!validateEntries()) return
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch(Dispatchers.Main) {
            val expense = prepareExpenseObjectFromEnteredValues()
            withContext(Dispatchers.IO) {
                ExpenseCategories.checkAndAdd(ExpenseCategory(name = expense.category))
                Expenses.add(expense) { success, error ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (success) {
                            CommonUtils.showMessage(context, "Expense submitted", "Expense / Expense ${expense.category} has been submitted")
                            SharedViewModelSingleton.expenseSubmittedEvent.postValue(expense)
                            NotificationUtils.expenseSubmitted(expense)
                        } else CommonUtils.showMessage(context, "Expense Error", "Error occurred while submitting the expense: $error")
                        progressBar.dismiss()
                        finish()
                    }
                }
            }
        }
    }

    private fun updateExpense() {
        if (!validateEntries()) return
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch(Dispatchers.Main) {
            val expense = prepareExpenseObjectFromEnteredValues()
            withContext(Dispatchers.IO) {
                ExpenseCategories.checkAndAdd(ExpenseCategory(name = expense.category))
                Expenses.update(expense) { success, error ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        progressBar.dismiss()
                        if (success) {
                            CommonUtils.toastMessage(context, "Expense ${expense.category} has been updated.")
                            SharedViewModelSingleton.expenseUpdatedEvent.postValue(expense)
                            NotificationUtils.expenseUpdated(expense)
                            finish()
                        } else CommonUtils.showMessage(context, "Expense Error", "Error occurred while updating the expense: $error")
                    }
                }
            }
        }
    }

    private fun removeExpense() {
        ExpenseActions(context, expense ?: return).removeExpense { success, _ ->
            if (success) finish()
        }
    }
}