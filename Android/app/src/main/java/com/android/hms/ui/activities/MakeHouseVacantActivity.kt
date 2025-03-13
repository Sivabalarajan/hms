package com.android.hms.ui.activities

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Houses
import com.android.hms.model.Users
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MakeHouseVacantActivity : BaseActivity() {

    private val house = SharedViewModelSingleton.currentHouseObject

    private lateinit var etVacateDate: EditText
    private lateinit var etDeposit: EditText
    private lateinit var etRent: EditText
    private lateinit var etNotes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_house_vacant)
        setActionBarView("Make House Vacant - Remove Tenant")

        if (house == null) {
            CommonUtils.showMessage(context, "House", "Not able to get the house details. Please try again later.")
            return
        }
        SharedViewModelSingleton.currentHouseObject = null

        findViewById<TextView>(R.id.etHouseDetails).text = "House: ${house.name} in ${house.bName}\nTenant: ${house.tName}, joined on ${CommonUtils.getFullDayDateFormatText(house.tJoined)}.\n\nThis will set the house as vacant. Please select the date from which house is vacant."

        etVacateDate = findViewById(R.id.etVacateDate)
        etDeposit = findViewById(R.id.etDeposit)
        etRent = findViewById(R.id.etRent)
        etNotes = findViewById(R.id.etNotes)

        etDeposit.setText("${house.deposit}")
        etRent.setText("${house.rent}")
        etNotes.setText(house.notes)
        setActionBarView("Make ${house.name} Vacant - Remove Tenant")

        CommonUtils.pickAndSetDate(etVacateDate)
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnMakeHouseVacant).setOnClickListener { confirmMakingHouseVacant() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { return true }

    private fun confirmMakingHouseVacant() {
        val alertDialog = CommonUtils.confirmMessage(context, "Make House Vacant", "Are you sure you want to make the house vacant? Please confirm.", "Make House Vacant")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            makeHouseVacant()
            alertDialog.dismiss()
        }
    }

    private fun makeHouseVacant() {
        val house = house
        if (house == null) {
            CommonUtils.showMessage(context, "House", "Not able to get the house details. Please try again later.")
            return
        }
        val currentTime = CommonUtils.currentTime
        val vacatedDateInMilli = etVacateDate.tag as Long
        if (vacatedDateInMilli == 0L || vacatedDateInMilli > currentTime || vacatedDateInMilli < house.tJoined) {
            CommonUtils.showMessage(context, "Valid date", "Please select a valid house vacated date. It should be between today's date and occupied date")
            return
        }
        val deposit = etDeposit.text.toString().trim().toIntOrNull()
        if (deposit == null || deposit <= 0) {
            CommonUtils.showMessage(context, "Valid deposit", "Please enter a valid security deposit amount.")
            return
        }
        val rent = etRent.text.toString().trim().toIntOrNull()
        if (rent == null || rent <= 0) {
            CommonUtils.showMessage(context, "Valid rent", "Please enter a valid rent amount.")
            return
        }
        val notes = etNotes.text.toString().trim()
        val canRemoveTenant = findViewById<CheckBox>(R.id.chkRemoveTenant).isChecked
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch(Dispatchers.IO) {
            if (rent != house.rent || house.rRevised == 0L) {
                house.rent = rent
                house.deposit = deposit
                house.rRevised = vacatedDateInMilli
            }
            house.notes = notes
            val tenantId = house.tId
            val tenantName = house.tName
            if (canRemoveTenant && Houses.getTenantHouses(house.tId).count() > 1) {
                lifecycleScope.launch(Dispatchers.Main) {
                    CommonUtils.showMessage(context, "Tenant with more houses", "The $tenantName is associated with more than one house. Please de-associate from other houses before removing from application.")
                    progressBar.dismiss()
                }
                return@launch
            }
            Houses.makeVacant(house, vacatedDateInMilli)
            NotificationUtils.houseVacated(house)
            if (canRemoveTenant) {
                Users.delete(tenantId, tenantName) { success, error ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        CommonUtils.showMessage(context, "House vacation and tenant removal", if (success) "House has been vacated and tenant has been removed" else "Error occurred: $error")
                        if (success) NotificationUtils.userRemoved(tenantName, "Tenant")
                        progressBar.dismiss()
                        finish()
                    }
                }
            }
            else {
                lifecycleScope.launch(Dispatchers.Main) {
                    CommonUtils.toastMessage(context, "House has been vacated and tenant $tenantName has been de-associated from the house")
                    progressBar.dismiss()
                    finish()
                }
            }
            SharedViewModelSingleton.houseUpdatedEvent.postValue(house)
        }
    }
}