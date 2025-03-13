package com.android.hms.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.model.Repair
import com.android.hms.model.RepairDescription
import com.android.hms.model.RepairDescriptions
import com.android.hms.model.Repairs
import com.android.hms.model.Users
import com.android.hms.ui.RepairActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RepairActivity: BaseActivity() {

    private var buildingId = ""
    private var buildingName = ""
    private var repairId = ""
    private var repair: Repair? = null
    private var house: House? = null

    private lateinit var tvRepairDescription: AutoCompleteTextView
    private lateinit var etRepairNotes: EditText
    private lateinit var etRepairAmount: EditText
    private lateinit var etPaymentType: EditText
    private lateinit var spRepairStatus: Spinner
    private lateinit var etRaisedDate: EditText
    private lateinit var etFixedDate: EditText
    private lateinit var etPaidDate: EditText
    private lateinit var etRaisedBy: EditText
    private lateinit var etFixedBy: EditText
    private lateinit var btnSubmitRepair: Button
    private lateinit var btnEditRepair: Button
    private lateinit var btnCloseRepair: Button
    private lateinit var btnRemoveRepair: Button
    private lateinit var layoutEdit: LinearLayout
    private lateinit var chkTenantPaid: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repair)
        setActionBarView("Building Repair")

        repairId = intent.getStringExtra(Globals.gFieldId) ?: ""

        buildingId = intent.getStringExtra("buildingId") ?: ""
        buildingName = intent.getStringExtra("buildingName") ?: ""
        if (buildingId.isEmpty() || buildingName.isEmpty()) {
            CommonUtils.showMessage(context, "Building", "Not able to get the building details. Please try again later.")
            return
        }

        val progressBar = MyProgressBar(context)
        chkTenantPaid = findViewById(R.id.chkTenantPaid)

        val houseId = intent.getStringExtra("houseId") ?: ""
        if (houseId.isNotEmpty()) {
            house = Houses.getById(houseId) ?: return
            buildingId = house?.bId ?: return
            buildingName = house?.bName ?: return
            setActionBarView("House ${house?.name} Repair")
            findViewById<TextView>(R.id.etBuildingHouseDetails).text = if (repairId.isEmpty()) "Submit repair details on house ${house?.name} in ${house?.bName}"
                else "Update repair details on house ${house?.name} in ${house?.bName}"
        }
        else {
            setActionBarView("Building $buildingName Repair")
            chkTenantPaid.visibility = View.GONE
            findViewById<TextView>(R.id.etBuildingHouseDetails).text = if (repairId.isEmpty()) "Submit repair details on building $buildingName"
                else "Update repair details on building $buildingName"
        }

        initObjects()
        progressBar.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { return true }

    private fun initObjects() {
        val progressBar = MyProgressBar(context)
        tvRepairDescription = findViewById(R.id.tvRepairDescription)
        etRepairNotes = findViewById(R.id.etRepairNotes)
        etRepairAmount = findViewById(R.id.etRepairAmount)
        etPaymentType = findViewById(R.id.etPaymentType)
        spRepairStatus = findViewById(R.id.spRepairStatus)
        etRaisedDate = findViewById(R.id.etRaisedDate)
        etFixedDate = findViewById(R.id.etFixedDate)
        etPaidDate = findViewById(R.id.etPaidDate)
        etRaisedBy = findViewById(R.id.etRaisedBy)
        etFixedBy = findViewById(R.id.etFixedBy)
        btnEditRepair = findViewById(R.id.btnEditRepair)
        btnSubmitRepair = findViewById(R.id.btnSubmitRepair)
        btnCloseRepair = findViewById(R.id.btnCloseRepair)
        btnRemoveRepair = findViewById(R.id.btnRemoveRepair)
        layoutEdit = findViewById(R.id.layoutEdit)

        CommonUtils.pickAndSetDate(etRaisedDate)
        CommonUtils.pickAndSetDate(etFixedDate)
        CommonUtils.pickAndSetDate(etPaidDate)

        val statusAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, Repairs.statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRepairStatus.adapter = statusAdapter

        val descriptions = RepairDescriptions.descriptions.sorted()
        val descriptionAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, descriptions)
        tvRepairDescription.setAdapter(descriptionAdapter)
        tvRepairDescription.setOnClickListener { tvRepairDescription.showDropDown() }
        tvRepairDescription.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) tvRepairDescription.showDropDown() }

        btnSubmitRepair.setOnClickListener { if (repair == null) initiateNewRepair() else updateRepair() }
        btnEditRepair.setOnClickListener { if (repair == null) initiateNewRepair() else updateRepair() }
        btnCloseRepair.setOnClickListener { closeRepair() }
        btnRemoveRepair.setOnClickListener { removeRepair() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }

        setValuesForNew()

        if (repairId.isEmpty()) {
            findViewById<Button>(R.id.btnTapExpense).setOnClickListener { showExpenseScreen() }
            progressBar.dismiss()
            return
        }
        findViewById<Button>(R.id.btnTapExpense).visibility = View.GONE

        repair = SharedViewModelSingleton.currentRepairObject
        SharedViewModelSingleton.currentRepairObject = null
        if (repair == null) repair = Repairs.getByIdLocally(repairId)
        lifecycleScope.launch {
            if (repair == null) repair = withContext(Dispatchers.IO) { Repairs.getByIdFromDb(repairId) }
            setValuesForExisting()
            progressBar.dismiss()
        }
    }

    private fun setValuesForNew() {
        if (repairId.isNotEmpty()) return

        spRepairStatus.setSelection(0) // default selection
        CommonUtils.setDate(etRaisedDate, CommonUtils.currentTime) // default
        etRaisedBy.setText(Users.currentUser?.name) // default user
        chkTenantPaid.isChecked = house?.tId?.isNotEmpty() ?: false
        layoutEdit.visibility = View.GONE
    }

    private fun setValuesForExisting() {
        val repair = repair ?: return
        tvRepairDescription.setText(repair.desc)
        etRepairNotes.setText(repair.notes)
        etRepairAmount.setText("${repair.amount}")
        etPaymentType.setText(repair.paidType)
        spRepairStatus.setSelection(Repairs.statuses.indexOfFirst { it == repair.status })
        etRaisedBy.setText(repair.raisedBy)
        etFixedBy.setText(repair.fixedBy)

        CommonUtils.setDate(etRaisedDate, repair.raisedOn)
        CommonUtils.setDate(etFixedDate, repair.fixedOn)
        CommonUtils.setDate(etPaidDate, repair.paidOn)

        chkTenantPaid.isChecked = repair.tPaid

        btnSubmitRepair.visibility = View.GONE
    }

    private fun validateEntries() : Boolean {
        val errorMessage = StringBuilder()
        val currentTime = CommonUtils.currentTime
        val raisedDate = etRaisedDate.tag as Long
        var fixedDate = etFixedDate.tag as Long
        val paidDate = etPaidDate.tag as Long
        if (raisedDate == 0L || raisedDate > currentTime) errorMessage.append("Please select valid raised date.\n")
        val repairAmount = etRepairAmount.text.toString().trim().toDoubleOrNull()
        if (repairAmount == null || repairAmount < 0) errorMessage.append("Please enter valid repair amount.\n")
        if (etRaisedBy.text.toString().trim().isEmpty()) errorMessage.append("Please enter valid person name in raised by.\n")
        if (tvRepairDescription.text.toString().trim().isEmpty()) errorMessage.append("Please enter valid repair description.\n")
        // if (etRepairDesc.text.toString().trim().isEmpty()) errorMessage.append("Please enter valid repair description.\n")
        // if (tvRepairCategory.text.toString().trim().isEmpty()) errorMessage.append("Please enter or choose valid repair category.\n")
        // if (etRepairNotes.text.toString().trim().isEmpty()) errorMessage.append("Please enter valid repair notes.\n")

        val statusSelection = spRepairStatus.selectedItemPosition
        if (fixedDate > 0L && statusSelection < 3) spRepairStatus.setSelection(3)
        if (paidDate > 0L && statusSelection < 4) spRepairStatus.setSelection(4)
        if (paidDate > 0 && fixedDate == 0L) {
            fixedDate = paidDate
            CommonUtils.setDate(etFixedDate, fixedDate)
        }

        val status = spRepairStatus.selectedItem.toString().trim()
        if (status.isEmpty()) errorMessage.append("Please choose a valid repair status.\n")
        else if (status == Repairs.statuses[3] || status == Repairs.statuses[4]) {
            if (fixedDate == 0L || fixedDate > currentTime ) errorMessage.append("Please select a valid fixed date.\n")
            if (raisedDate > fixedDate) errorMessage.append("Raised date should not be greater than fixed date.\n")
            if (etFixedBy.text.toString().trim().isEmpty()) errorMessage.append("Please enter a valid person name in fixed by.\n")
            if (status == Repairs.statuses[4]) { // if paid is chosen...
                if (paidDate == 0L || paidDate > currentTime ) errorMessage.append("Please select a valid paid date.\n")
                if (raisedDate > paidDate) errorMessage.append("Raised date should not be greater than paid date.\n")
                if (repairAmount == 0.0) errorMessage.append("Please enter valid repair amount.\n")
                if (fixedDate > paidDate) errorMessage.append("Fixed date should not be greater than paid date.\n")
            }
        }

        if (errorMessage.isNotEmpty()) {
            CommonUtils.showMessage(context, "Invalid entry / entries", errorMessage.toString())
            return false
        }

        return true
    }

    private fun prepareRepairObjectFromEnteredValues(): Repair {
        val raisedOn = etRaisedDate.tag as Long
        val repairAmount = etRepairAmount.text.toString().trim().toDouble()
        val raisedBy = etRaisedBy.text.toString().trim()
        val desc = tvRepairDescription.text.toString().trim()
        val notes = etRepairNotes.text.toString().trim()
        val status = spRepairStatus.selectedItem.toString().trim()
        val paymentType = etPaymentType.text.toString().trim()
        val fixedOn = etFixedDate.tag as Long
        val paidOn = etPaidDate.tag as Long
        val fixedBy = etFixedBy.text.toString().trim()

        val repair = repair ?: return Repair(desc = desc, notes = notes, tPaid = false, bId = buildingId, bName = buildingName,
            paidOn = paidOn, fixedOn = fixedOn, raisedOn = raisedOn, raisedBy = raisedBy, fixedBy = fixedBy, amount = repairAmount, paidType = paymentType, status = status)

        repair.desc = desc
        repair.notes = notes
        repair.paidOn = paidOn
        repair.fixedOn = fixedOn
        repair.raisedOn = raisedOn
        repair.raisedBy = raisedBy
        repair.fixedBy = fixedBy
        repair.amount = repairAmount
        repair.paidType = paymentType
        repair.status = status

        repair.tPaid = chkTenantPaid.isChecked
        repair.tId = house?.tId ?: ""
        repair.tName = house?.tName ?: ""
        repair.hId = house?.id ?: ""
        repair.hName = house?.name ?: ""

        return repair
    }

    private fun initiateNewRepair() {
        if (!validateEntries()) return
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch(Dispatchers.Main) {
            val repair = prepareRepairObjectFromEnteredValues()
            withContext(Dispatchers.IO) {
                RepairDescriptions.checkAndAdd(RepairDescription(desc = repair.desc))
                Repairs.add(repair) { success, error ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (success) {
                            CommonUtils.showMessage(context, "Repair submitted", "Repair ${repair.desc} has been submitted")
                            SharedViewModelSingleton.repairInitiatedEvent.postValue(repair)
                            NotificationUtils.repairSubmitted(repair)
                        } else CommonUtils.showMessage(context, "Repair Error", "Error occurred while submitting the repair: $error")
                        progressBar.dismiss()
                        finish()
                    }
                }
            }
        }
    }

    private fun updateRepair() {
        if (!validateEntries()) return
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch(Dispatchers.Main) {
            val repair = prepareRepairObjectFromEnteredValues()
            withContext(Dispatchers.IO) {
                RepairDescriptions.checkAndAdd(RepairDescription(desc = repair.desc))
                Repairs.update(repair) { success, error ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        progressBar.dismiss()
                        if (success) {
                            CommonUtils.toastMessage(context, "Repair ${repair.desc} has been updated.")
                            SharedViewModelSingleton.repairUpdatedEvent.postValue(repair)
                            NotificationUtils.repairUpdated(repair)
                            finish()
                        } else CommonUtils.showMessage(context, "Repair Error", "Error occurred while updating the repair: $error")
                    }
                }
            }
        }
    }

    private fun closeRepair() {
        val notes = etRepairNotes.text.toString().trim()
        RepairActions(context, repair ?: return).closeRepair(notes) { success, _ ->
            if (success) finish()
        }
    }

    private fun removeRepair() {
        RepairActions(context, repair ?: return).removeRepair { success, _ ->
            if (success) finish()
        }
    }

    private fun showExpenseScreen() {
        LaunchUtils.showExpenseActivity(this, buildingId, buildingName, house?.id ?: "")
        finish()
    }
}