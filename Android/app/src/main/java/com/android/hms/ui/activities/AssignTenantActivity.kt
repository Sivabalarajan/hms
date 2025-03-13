package com.android.hms.ui.activities

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Houses
import com.android.hms.model.User
import com.android.hms.model.Users
import com.android.hms.ui.adapters.SelectTenantArrayAdapter
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssignTenantActivity : BaseActivity() {

    private val house = SharedViewModelSingleton.currentHouseObject

    private lateinit var chkExistingTenant: CheckBox
    private lateinit var etTenantName: EditText
    private lateinit var etTenantPhone: EditText
    private lateinit var etTenantEmailId: EditText
    private lateinit var etTenantAddress: EditText
    private lateinit var etTenantNotes: EditText
    private lateinit var etRentedDate: EditText
    private lateinit var etHouseDeposit: EditText
    private lateinit var etHouseRent: EditText
    private lateinit var etHouseNotes: EditText
    private lateinit var chkHouseDepositPaid: CheckBox

    private var selectedTenant: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_tenant_to_house)
        setActionBarView("Assign Tenant to House")

        if (house == null) {
            CommonUtils.showMessage(context, "House", "Not able to get the house details. Please try again later.")
            return
        }
        SharedViewModelSingleton.currentHouseObject = null

        val lastTenantLeft = if (house.tLeft == 0L) "Not found" else CommonUtils.getFullDayDateFormatText(house.tLeft)
        findViewById<TextView>(R.id.etHouseDetails).text = "House: ${house.name} in ${house.bName}\nLast tenant left on: $lastTenantLeft.\n\nThis will set the house as rented. Please select the tenant and date from which house is rented."

        chkExistingTenant = findViewById(R.id.chkExistingTenant)
        etTenantName = findViewById(R.id.etTenantName)
        etTenantPhone = findViewById(R.id.etTenantPhone)
        etTenantEmailId = findViewById(R.id.etTenantEmailId)
        etTenantAddress = findViewById(R.id.etTenantAddress)
        etTenantNotes = findViewById(R.id.etTenantNotes)
        etRentedDate = findViewById(R.id.etRentedDate)
        etHouseDeposit = findViewById(R.id.etHouseDeposit)
        etHouseRent = findViewById(R.id.etHouseRent)
        etHouseNotes = findViewById(R.id.etHouseNotes)
        chkHouseDepositPaid = findViewById(R.id.chkHouseDepositPaid)

        etHouseDeposit.setText("${house.deposit}")
        etHouseRent.setText("${house.rent}")
        etHouseNotes.setText(house.notes)

        CommonUtils.pickAndSetDate(etRentedDate)
        setActionBarView("Assign Tenant to ${house.name}")

        chkExistingTenant.setOnClickListener { checkForSelectingExistingTenant() }
        findViewById<Button>(R.id.btnMakeHouseRented).setOnClickListener { makeHouseRented() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { return true }

    private fun checkForSelectingExistingTenant() {
        if (!chkExistingTenant.isChecked) {
            etTenantName.isEnabled = true
            etTenantPhone.isEnabled = true
            etTenantEmailId.isEnabled = true
            return
        }
        launch {
            val progressBar = MyProgressBar(context)
            val tenants = Users.getTenants()
            progressBar.dismiss()
            if (tenants.isEmpty()) {
                CommonUtils.toastMessage(context, "No tenants available. Please add a one.")
                chkExistingTenant.isChecked = false
                chkExistingTenant.isEnabled = false
                return@launch
            }
            showSelectTenantDialog(tenants) { tenant ->
                selectedTenant = tenant
                if (tenant == null) {
                    chkExistingTenant.isChecked = false
                    return@showSelectTenantDialog
                }
                etTenantName.isEnabled = false
                etTenantPhone.isEnabled = false
                etTenantEmailId.isEnabled = false
                etTenantName.setText(tenant.name)
                etTenantPhone.setText(tenant.phone)
                etTenantEmailId.setText(tenant.email)
                etTenantAddress.setText(tenant.address)
                etTenantNotes.setText(tenant.notes)
            }
        }
    }

    private fun makeHouseRented() {
        val house = house
        if (house == null) {
            CommonUtils.showMessage(context, "House", "Not able to get the house details. Please try again later.")
            return
        }
        val currentTime = CommonUtils.currentTime
        val rentedDateInMillis = etRentedDate.tag as Long
        if (rentedDateInMillis == 0L || rentedDateInMillis > currentTime) {
            CommonUtils.showMessage(context, "Valid date", "Please select a valid house rented / to be rented date.")
            return
        }
        val deposit = etHouseDeposit.text.toString().trim().toIntOrNull()
        if (deposit == null || deposit <= 0) {
            CommonUtils.showMessage(context, "Valid deposit", "Please enter a valid security deposit.")
            return
        }
        val rent = etHouseRent.text.toString().trim().toIntOrNull()
        if (rent == null || rent <= 0) {
            CommonUtils.showMessage(context, "Valid rent", "Please enter a valid rent.")
            return
        }
        val tenantName = etTenantName.text.toString().trim()
        val tenantEmail = etTenantEmailId.text.toString().trim()
        val tenantPhone = etTenantPhone.text.toString().trim()
        val tenantAddress = etTenantAddress.text.toString().trim()
        val tenantNotes = etTenantNotes.text.toString().trim()
        val isExistingTenant = chkExistingTenant.isChecked
        val isHouseDepositPaid = chkHouseDepositPaid.isChecked
        if (isExistingTenant) {
            if (selectedTenant == null) {
                CommonUtils.showMessage(context, "Existing tenant", "Not able to get existing tenant details.")
                return
            }
        } else {
            if (tenantName.isEmpty() || (tenantEmail.isEmpty() && tenantPhone.isEmpty())) {
                CommonUtils.showMessage(context, "New Tenant Details", "Please enter valid tenant details. Name should not be empty and either email or phone is required.")
                return
            }
            if (tenantEmail.isNotEmpty()) {
                if (!CommonUtils.isValidEmail(tenantEmail)) {
                    CommonUtils.showMessage(context, "Invalid Email", "Please enter valid email address.")
                    return
                }
                if (Users.doesEmailExist(tenantEmail)) {
                    CommonUtils.showMessage(context, "New Tenant Exists", "The new tenant's email already exists.")
                    return
                }
            }
            if (tenantPhone.isNotEmpty()) {
                if (Users.doesPhoneExist(tenantPhone)) {
                    CommonUtils.showMessage(context, "New Tenant Exists", "he new tenant's phone already exists.")
                    return
                }
            }
        }
        val notes = etHouseNotes.text.toString().trim()
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch(Dispatchers.IO) {
            if (isExistingTenant) {
                val tenant = selectedTenant ?: return@launch
                Houses.makeRented(house, tenant, rent, deposit, isHouseDepositPaid, notes, rentedDateInMillis)
                if (tenant.address != tenantAddress || tenant.notes != tenantNotes) {
                    tenant.address = tenantAddress
                    tenant.notes = tenantNotes
                    Users.update(tenant)
                }
                lifecycleScope.launch(Dispatchers.Main) {
                    SharedViewModelSingleton.houseUpdatedEvent.postValue(house)
                    progressBar.dismiss()
                    finish()
                }
            } else {
                val tenant = User(name = tenantName, email = tenantEmail, phone = tenantPhone, address = tenantAddress, notes = tenantNotes)
                Users.add(tenant) { success, error ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (success) {
                            SharedViewModelSingleton.userAddedEvent.postValue(tenant)
                            NotificationUtils.userAdded(tenant)
                            withContext(Dispatchers.IO) { Houses.makeRented(house, tenant, rent, deposit, isHouseDepositPaid, notes, rentedDateInMillis) }
                            NotificationUtils.houseRented(house)
                            SharedViewModelSingleton.houseUpdatedEvent.postValue(house)
                            finish()
                        } else CommonUtils.showMessage(context, "Not able to add", "Not able to add the new tenant. Please try again later. Error: $error")
                        progressBar.dismiss()
                    }
                }
            }
        }
    }

    private fun showSelectTenantDialog(tenantsList: List<User>, onTenantSelected: (User?) -> Unit) {
        val builder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_searchable_tenants, null)
        builder.setView(dialogView)
        builder.setCancelable(false) // Prevent closing without action
        val dialog = builder.create()

        val listView: ListView = dialogView.findViewById(R.id.listView)
        val searchView: SearchView = dialogView.findViewById(R.id.searchView)
        listView.adapter = SelectTenantArrayAdapter(context, tenantsList)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                listView.adapter = SelectTenantArrayAdapter(context, tenantsList.filter { it.name.contains(newText ?: "", ignoreCase = true) })
                return true
            }
        })

        selectedTenant = null
        listView.setOnItemClickListener { _, _, position, _ ->
            // TODO: Change the color of the selected item if required
            val adapter = listView.adapter as SelectTenantArrayAdapter
            selectedTenant = adapter.getItem(position) as User
            adapter.setSelectedPosition(position)
//            onTenantSelected(selectedTenant) // Return the selected name
//            builder.create().dismiss() // Close the dialog
        }

        dialogView.findViewById<Button>(R.id.btnSelect).setOnClickListener {
            val tenant = selectedTenant
            if (tenant == null) {
                CommonUtils.showMessage(context, "Select a tenant", "Please select a tenant.")
                return@setOnClickListener
            }
            onTenantSelected(tenant) // Return the selected tenant
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            onTenantSelected(null)
            dialog.dismiss()
        }

        dialog.show()
    }
}