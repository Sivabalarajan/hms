package com.android.hms.ui.activities

import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.House
import com.android.hms.model.Houses
import com.android.hms.ui.HouseActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddHouseActivity : BaseActivity() {

    private lateinit var etName: EditText
    // private lateinit var etAddress: EditText
    private lateinit var etRent: EditText
    private lateinit var etDeposit: EditText
    private lateinit var etNotes: EditText

    private lateinit var buildingId: String
    private lateinit var buildingName: String
    private var house: House? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_house)

        buildingId = intent.getStringExtra("buildingId") ?: ""
        buildingName = intent.getStringExtra("buildingName") ?: ""

        if (buildingId.isEmpty() || buildingName.isEmpty()) {
            CommonUtils.showMessage(context, "Building", "Not able to associate with any building. Please try again later.")
            return
        }

        val progressBar = MyProgressBar(context)
        val houseId = intent.getStringExtra(Globals.gFieldId) ?: ""

        findViewById<TextView>(R.id.tvBuildingName).text = if (houseId.isEmpty()) "This house will be added in '$buildingName'" else "House is in $buildingName"
        etName = findViewById(R.id.etHouseName)
        // etAddress = findViewById(R.id.etHouseAddress)
        etRent = findViewById(R.id.etRent)
        etDeposit = findViewById(R.id.etDeposit)
        etNotes = findViewById(R.id.etNotes)

        val addButton = findViewById<Button>(R.id.btnAddHouse)
        val anotherButton = findViewById<Button>(R.id.btnAnotherHouse)
        val cancelButton = findViewById<Button>(R.id.btnCancel)

        lifecycleScope.launch {
            if (houseId.isNotEmpty()) house = withContext(Dispatchers.IO) { Houses.getById(houseId) }
            if (house != null) {
                setActionBarView("Change house ${house?.name} details")
                etName.setText(house?.name)
                // etAddress.setText(house?.address)
                etRent.setText("${house?.rent}")
                etDeposit.setText("${house?.deposit}")
                etNotes.setText(house?.notes)

                addButton.text = "Update House"
                addButton.setOnClickListener { updateHouse() }
                anotherButton.text = "Remove House"
                anotherButton.setOnClickListener { removeHouse() }
            } else {
                setActionBarView("Add house in $buildingName")
                addButton.setOnClickListener { addHouse() }
                anotherButton.setOnClickListener { addHouse(true) }
            }
            progressBar.dismiss()
        }

        cancelButton.setOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { return true }

    private fun addHouse(isAddAgain: Boolean = false) {
        val rent = etRent.text.toString().trim().toIntOrNull()
        if (rent == null || rent <= 0) {
            CommonUtils.showMessage(context, "Rent is a number", "Rent should have meaningful value")
            return
        }
        val deposit = etDeposit.text.toString().trim().toIntOrNull()
        if (deposit == null || deposit < 0) {
            CommonUtils.showMessage(context, "Deposit is a number", "Security deposit should have meaningful value")
            return
        }
        val name = etName.text.trim().toString()
        if (name.isEmpty() || /* house.address.isEmpty() || */ name.length < Globals.minFieldLength) { // || house.address.length < Globals.minFieldLength) {
            CommonUtils.showMessage(context, "Mandatory field(s)", "House name should not be empty and should have meaningful value.")
            return
        }
        val notes = etNotes.text.toString().trim()
        val progressBar = MyProgressBar(context, "Adding the house. Please wait...")
        lifecycleScope.launch(Dispatchers.IO) {
            val houseFromDb = Houses.getByNameInBuilding(name, buildingId)
            if (houseFromDb != null) {
                withContext(Dispatchers.Main) {
                    progressBar.dismiss()
                    CommonUtils.showMessage(context, "House already exists", "House '$name' already exists in this building $buildingName. Please try with some other name.")
                }
                return@launch
            }
            val house = House(
                name = name,
//            address = etAddress.text.toString().trim(),
                bId = buildingId,
                bName = buildingName,
                rent = rent,
                deposit = deposit,
                notes = notes
            )
            Houses.add(house) { success, error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.dismiss()
                    if (success) {
                        SharedViewModelSingleton.houseAddedEvent.postValue(house)
                        NotificationUtils.houseAdded(house)
                        if (isAddAgain) {
                            CommonUtils.toastMessage(context, "New house ${house.name} has been added. Please add another house.")
                            etName.setText("")
                            // etAddress.setText("")
                            etRent.setText("")
                            etDeposit.setText("")
                            etNotes.setText("")
                        } else {
                            CommonUtils.toastMessage(context, "New house ${house.name} is added")
                            finish()
                        }
                    } else {
                        CommonUtils.showMessage(context, "Not able to add", "Not able to add new house. $error")
                    }
                }
            }
        }
    }

    private fun updateHouse() {
        val house = house
        if (house == null) { // this should not happen
            CommonUtils.showMessage(context, "Unknown error", "Not able to get the house details to update.")
            return
        }
        val rent = etRent.text.toString().trim().toIntOrNull()
        if (rent == null || rent <= 0) {
            CommonUtils.showMessage(context, "Rent is a number", "Rent should have meaningful value")
            return
        }
        val deposit = etDeposit.text.toString().trim().toIntOrNull()
        if (deposit == null || deposit < 0) {
            CommonUtils.showMessage(context, "Deposit is a number", "Security deposit should have meaningful value")
            return
        }
        val oldName = house.name
        val newName = etName.text.trim().toString()
        if (house.name.isEmpty() || /* house.address.isEmpty() || */ house.name.length < Globals.minFieldLength) { // || house.address.length < Globals.minFieldLength) {
            CommonUtils.showMessage(context, "Mandatory field(s)", "House name should not be empty and should have meaningful value.")
            return
        }
        // val address = etAddress.text.toString().trim()
        val notes = etNotes.text.toString().trim()
        val progressBar = MyProgressBar(context, "Updating house details. Please wait...")
        lifecycleScope.launch(Dispatchers.IO) {
            if (oldName != newName) {
                val houseFromDb = Houses.getByNameInBuilding(newName, buildingId)
                if (houseFromDb != null) {
                    withContext(Dispatchers.Main) {
                        house.name = oldName // reset the name
                        progressBar.dismiss()
                        CommonUtils.showMessage(context, "House already exists", "House '$newName' already exists in this building $buildingName. Please try with some other name.")
                    }
                    return@launch
                }
            }
            house.name = newName
            house.rent = rent
            house.deposit = deposit
            // house.address = address
            house.notes = notes
            Houses.update(house) { success, error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.dismiss()
                    if (success) {
                        CommonUtils.toastMessage(context, "House ${house.name} details are updated")
                        SharedViewModelSingleton.houseUpdatedEvent.postValue(house)
                        finish()
                    } else {
                        CommonUtils.showMessage(context, "Not able to update", "Not able to update building details. $error")
                    }
                }
            }
        }
    }

    private fun removeHouse() {
        val house = house
        if (house == null) {
            CommonUtils.showMessage(context, "Not finding house", "Not able to get the house details. Please try again later.")
            return
        }
        HouseActions(context, house).removeHouse { success, _ ->
            if (success) finish()
        }
    }
}