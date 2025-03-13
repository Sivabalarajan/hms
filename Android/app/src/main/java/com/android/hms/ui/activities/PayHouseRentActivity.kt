package com.android.hms.ui.activities

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.model.Rent
import com.android.hms.model.Rents
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PayHouseRentActivity : BaseActivity() {

    private val house = SharedViewModelSingleton.currentHouseObject

    private lateinit var etRentAmountPaid: EditText
    private lateinit var  etRentDelay: EditText
    private lateinit var  etRentPaidDate: EditText
    private lateinit var  etRentNotes: EditText
    private lateinit var  chkHouseDepositPaid: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_house_rent)
        setActionBarView("Pay House Rent")

        if (house == null) {
            CommonUtils.showMessage(context, "House", "Not able to get the house details. Please try again later.")
            return
        }
        SharedViewModelSingleton.currentHouseObject = null

        val lastRentPaid = if (house.rPaid == 0L) "not found" else CommonUtils.getFullDayDateFormatText(house.rPaid)
        findViewById<TextView>(R.id.etHouseDetails).text =
            "${house.name} in ${house.bName}. ${house.tName}, occupied on ${CommonUtils.getFullDayDateFormatText(house.tJoined)}. Rent is $ ${CommonUtils.formatNumToText(house.rent)} and last paid is $lastRentPaid.\n\nPlease enter the details to mark rent as paid."

        etRentAmountPaid = findViewById(R.id.etRentAmountPaid)
        etRentDelay = findViewById(R.id.etRentDelay)
        etRentPaidDate = findViewById(R.id.etRentPaidDate)
        etRentNotes = findViewById(R.id.etRentNotes)
        chkHouseDepositPaid = findViewById(R.id.chkHouseDepositPaid)
        if (house.dPaid) chkHouseDepositPaid.visibility = View.GONE
        chkHouseDepositPaid.isChecked = house.dPaid
        etRentAmountPaid.setText("${house.rent}")

        setActionBarView("Pay Rent to ${house.name}")
        CommonUtils.pickAndSetDate(etRentPaidDate)

        findViewById<Button>(R.id.btnRentPaid).setOnClickListener { getRentPaid() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { return true }

    private fun getRentPaid() {
        val house = house
        if (house == null) {
            CommonUtils.showMessage(context, "House", "Not able to get the house details. Please try again later.")
            return
        }
        val currentTime = CommonUtils.currentTime
        val rentPaidDateInMillis = etRentPaidDate.tag as Long
        if (rentPaidDateInMillis == 0L || rentPaidDateInMillis > currentTime || rentPaidDateInMillis < house.tJoined) {
            CommonUtils.showMessage(context, "Valid date", "Please select a valid rent paid date. Rent can not be paid before occupying or in future.")
            return
        }
        val rentPaid = etRentAmountPaid.text.toString().trim().toIntOrNull()
        if (rentPaid == null || rentPaid == 0) {
            CommonUtils.showMessage(context, "Valid rent", "Please enter a valid rental amount paid.")
            return
        }
        val rentDelay = etRentDelay.text.toString().trim().toIntOrNull()
        if (rentDelay == null) {
            CommonUtils.showMessage(context, "Valid rent delay", "Please enter a valid rent delay, in days. Negative days indicates early payment.")
            return
        }
        val rentNotes = etRentNotes.text.toString().trim()
        val isHouseRentRevised = findViewById<CheckBox>(R.id.chkReviseHouseRent).isChecked
        val isHouseDepositPaid = chkHouseDepositPaid.isChecked
        val progressBar = MyProgressBar(context)
        lifecycleScope.launch(Dispatchers.IO) {
            val rent = Rent(
                hId = house.id,
                hName = house.name,
                bId = house.bId,
                bName = house.bName,
                tId = house.tId,
                tName = house.tName,
                amount = rentPaid,
                delay = rentDelay,
                paidOn = rentPaidDateInMillis,
                notes = rentNotes
            )
            Rents.add(rent) { success, error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    if (success) {
                        house.rPaid = rentPaidDateInMillis
                        house.dPaid = isHouseDepositPaid
                        if (rentPaid != house.rent && isHouseRentRevised) {
                            house.rent = rentPaid
                            house.rRevised = rentPaidDateInMillis
                        }
                        withContext(Dispatchers.IO) {
                            Houses.update(house) { success, err ->
                                lifecycleScope.launch(Dispatchers.Main) {
                                    SharedViewModelSingleton.houseUpdatedEvent.postValue(house)
                                    if (success) CommonUtils.toastMessage(context, "House rent has been paid and details are updated")
                                    else CommonUtils.showMessage(context, "House rent payment", "Error occurred in updating house details while paying rent: $err")
                                    progressBar.dismiss()
                                    finish()
                                }
                            }
                        }
                        SharedViewModelSingleton.rentPaidEvent.postValue(rent)
                        NotificationUtils.rentPaid(rent)
                    } else {
                        CommonUtils.showMessage(context, "Rent payment", "Error occurred in rent payment: $error")
                        progressBar.dismiss()
                    }
                }
            }
        }
    }
}