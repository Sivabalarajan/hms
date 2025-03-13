package com.android.hms.ui.activities

import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Building
import com.android.hms.model.Buildings
import com.android.hms.ui.BuildingActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddBuildingActivity : BaseActivity() {

    private lateinit var etBuildingName: EditText
    private lateinit var etBuildingAddress: EditText
    private lateinit var etBuildingArea: EditText
    private lateinit var etBuildingNotes: EditText

    private var building: Building? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_building)
        val progressBar = MyProgressBar(context)

        val buildingId = intent.getStringExtra(Globals.gFieldId) ?: ""

        etBuildingName = findViewById(R.id.etBuildingName)
        etBuildingAddress = findViewById(R.id.etBuildingAddress)
        etBuildingArea = findViewById(R.id.etBuildingArea)
        etBuildingNotes = findViewById(R.id.etBuildingNotes)

        val addButton = findViewById<Button>(R.id.btnAddBuilding)
        val anotherButton = findViewById<Button>(R.id.btnAnotherBuilding)
        val cancelButton = findViewById<Button>(R.id.btnCancel)

        lifecycleScope.launch {
            if (buildingId.isNotEmpty()) building = withContext(Dispatchers.IO) { Buildings.getById(buildingId) }
            if (building != null) {
                setActionBarView("Change building ${building?.name} details")
                etBuildingName.setText(building?.name)
                etBuildingAddress.setText(building?.address)
                etBuildingArea.setText(building?.area)
                etBuildingNotes.setText(building?.notes)

                addButton.text = "Update Building"
                addButton.setOnClickListener { updateBuilding() }
                anotherButton.text = "Remove Building"
                anotherButton.setOnClickListener { deleteBuilding() }
            } else {
                setActionBarView("Add New Building")
                addButton.setOnClickListener { addBuilding() }
                anotherButton.setOnClickListener { addBuilding(true) }
            }
            progressBar.dismiss()
        }

        cancelButton.setOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { return true }

    private fun addBuilding(isAddAgain: Boolean = false) {
        val building = Building(
            name = etBuildingName.text.trim().toString(),
            address = etBuildingAddress.text.toString().trim(),
            area = etBuildingArea.text.toString().trim(),
            notes = etBuildingNotes.text.toString().trim()
        )
        if (building.name.isEmpty() || building.address.isEmpty() || building.name.length < Globals.minFieldLength || building.address.length < Globals.minFieldLength) {
            CommonUtils.showMessage(context, "Mandatory field(s)", "Building name and address should not be empty and should have meaningful values.")
            return
        }
        val progressBar = MyProgressBar(context, "Adding the building. Please wait...")
        lifecycleScope.launch(Dispatchers.IO) {
            val buildingFromDb = Buildings.getByName(building.name)
            if (buildingFromDb != null) {
                withContext(Dispatchers.Main) {
                    progressBar.dismiss()
                    CommonUtils.showMessage(context, "Building already exists", "Building '${building.name}' already exists. Please try with some other name.")
                }
                return@launch
            }
            Buildings.add(building) { success, error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.dismiss()
                    if (success) {
                        SharedViewModelSingleton.buildingAddedEvent.postValue(building)
                        NotificationUtils.buildingAdded(building)
                        if (isAddAgain) {
                            CommonUtils.toastMessage(context, "New building ${building.name} has been added. Please add another building.")
                            etBuildingName.setText("")
                            etBuildingAddress.setText("")
                            etBuildingArea.setText("")
                            etBuildingNotes.setText("")
                        } else {
                            CommonUtils.toastMessage(context, "New building ${building.name} is added")
                            finish()
                        }
                    } else {
                        CommonUtils.showMessage(context, "Not able to add", "Not able to add new building. $error")
                    }
                }
            }
        }
    }

    private fun updateBuilding() {
        if (building == null) { // this should not happen
            CommonUtils.showMessage(context, "Unknown error", "Not able to get the building details to update.")
            return
        }
        val building = building ?: return
        val oldName = building.name
        val newName = etBuildingName.text.trim().toString()
        val address = etBuildingAddress.text.toString().trim()
        if (newName.isEmpty() || address.isEmpty() || newName.length < Globals.minFieldLength || address.length < Globals.minFieldLength) {
            CommonUtils.showMessage(context, "Mandatory field(s)", "Building name and address should not be empty and should have meaningful values.")
            return
        }
        val area = etBuildingArea.text.toString().trim()
        val notes = etBuildingNotes.text.toString().trim()
        val progressBar = MyProgressBar(context, "Updating building details. Please wait...")
        lifecycleScope.launch(Dispatchers.IO) {
            if (oldName != newName) {
                val buildingFromDb = Buildings.getByName(newName)
                if (buildingFromDb != null) {
                    withContext(Dispatchers.Main) {
                        building.name = oldName // reset the name
                        progressBar.dismiss()
                        CommonUtils.showMessage(context, "Building already exists", "Building '$newName' already exists. Please try with some other name.")
                    }
                    return@launch
                }
            }
            building.name = newName
            building.address = address
            building.area = area
            building.notes = notes
            Buildings.update(building) { success, error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.dismiss()
                    if (success) {
                        SharedViewModelSingleton.buildingUpdatedEvent.postValue(building)
                        CommonUtils.toastMessage(context, "Building ${building.name} details are updated")
                        finish()
                    } else {
                        CommonUtils.showMessage(context, "Not able to update", "Not able to update building details. $error")
                    }
                }
            }
        }
    }

    private fun deleteBuilding() {
        BuildingActions.removeBuilding(context, building ?: return) { success, _ ->
            if (success) finish()
        }
    }
}