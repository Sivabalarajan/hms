package com.android.hms.ui.activities

import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Houses
import com.android.hms.model.User
import com.android.hms.model.Users
import com.android.hms.ui.UserActions
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddUserActivity : BaseActivity() {

    private var user: User? = null

    private lateinit var radioGroupUserRole : RadioGroup
    private lateinit var radioOwner : RadioButton
    private lateinit var radioHelper : RadioButton
    private lateinit var radioTenant : RadioButton
    private lateinit var etUserName : EditText
    private lateinit var etUserPhone : EditText
    private lateinit var etUserEmail : EditText
    private lateinit var etUserAddress : EditText
    private lateinit var etUserNotes : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        radioGroupUserRole = findViewById(R.id.radioGroupUserRole)
        radioOwner = findViewById(R.id.radioOwner)
        radioHelper = findViewById(R.id.radioHelper)
        radioTenant = findViewById(R.id.radioTenant)
        etUserName = findViewById(R.id.etUserName)
        etUserPhone = findViewById(R.id.etUserPhone)
        etUserEmail = findViewById(R.id.etUserEmail)
        etUserAddress = findViewById(R.id.etUserAddress)
        etUserNotes = findViewById(R.id.etUserNotes)

        val userId = intent.getStringExtra(Globals.gFieldId) ?: ""

        val addButton = findViewById<Button>(R.id.btnAddUser)
        val anotherButton = findViewById<Button>(R.id.btnAnotherUser)
        val cancelButton = findViewById<Button>(R.id.btnCancel)

        val progressBar = MyProgressBar(this)
        lifecycleScope.launch {
            if (userId.isNotEmpty()) user = withContext(Dispatchers.IO) { Users.getById(userId) }
            if (user != null) {
                setActionBarView("Change ${user?.name} Details")
                user?.let {
                    etUserName.setText(it.name)
                    etUserEmail.setText(it.email)
                    setSelectedRole(it.role)
                    etUserAddress.setText(it.address)
                    etUserPhone.setText(it.phone)
                    etUserNotes.setText(it.notes)
                }
                if (user?.role != Users.Roles.HELPER.value) {
                    enableRolesViews(false)
                    enableRoleViewIfNeeded() // do not change the role if the user is tenant and associated with house(s)
                }
                addButton.text = "Update User"
                addButton.setOnClickListener { updateUser() }
                anotherButton.text = "Remove User"
                if (user?.id == Users.currentUser?.id) anotherButton.isEnabled = false
                else anotherButton.setOnClickListener { deleteUser() }
            } else {
                setActionBarView("Add User")
                addButton.setOnClickListener { addUser() }
                anotherButton.setOnClickListener { addUser(true) }
            }
            progressBar.dismiss()
        }

        cancelButton.setOnClickListener { finish() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { return true }

    private fun enableRoleViewIfNeeded() {
        lifecycleScope.launch(Dispatchers.Main) {
            val user = user ?: return@launch
            if (user.role == Users.Roles.TENANT.value) enableRolesViews(Houses.getTenantHouses(user.id).isEmpty())
        }
    }

    private fun enableRolesViews(bEnable: Boolean = true) {
        radioGroupUserRole.isEnabled = bEnable
        radioOwner.isEnabled = bEnable
        radioHelper.isEnabled = bEnable
        radioTenant.isEnabled = bEnable
    }

    private fun getSelectedRole() : String {
        return when (radioGroupUserRole.checkedRadioButtonId) {
            R.id.radioOwner -> Users.Roles.OWNER.value
            R.id.radioHelper -> Users.Roles.HELPER.value
            R.id.radioTenant -> Users.Roles.TENANT.value
            else -> Users.Roles.HELPER.value // default selection
        }
    }

    private fun setSelectedRole(userRole: String) {
        when (userRole) {
            Users.Roles.OWNER.value -> radioGroupUserRole.check(R.id.radioOwner)
            Users.Roles.HELPER.value -> radioGroupUserRole.check(R.id.radioHelper)
            Users.Roles.TENANT.value -> radioGroupUserRole.check(R.id.radioTenant)
            else -> radioGroupUserRole.check(R.id.radioHelper) // default selection
        }
    }

    private fun addUser(isAddAgain: Boolean = false) {
        val userName = etUserName.text.trim().toString()
        val userEmail = etUserEmail.text.trim().toString()
        val userPhone = etUserPhone.text.trim().toString()
        if (userName.isEmpty() || (userEmail.isEmpty() && userPhone.isEmpty())) {
            CommonUtils.showMessage(context, "New User Details", "Please enter valid user details. Name should not be empty and either email or phone is required.")
            return
        }
        if (userEmail.isNotEmpty()) {
            if (!CommonUtils.isValidEmail(userEmail)) {
                CommonUtils.showMessage(context, "Invalid Email", "Please enter valid email address.")
                return
            }
            if (Users.doesEmailExist(userEmail)) {
                CommonUtils.showMessage(context, "New User Exists", "The new user's email already exists.")
                return
            }
        }
        if (userPhone.isNotEmpty()) {
            if (Users.doesPhoneExist(userPhone)) {
                CommonUtils.showMessage(context, "New User Exists", "he new user's phone already exists.")
                return
            }
        }
        val userRole = getSelectedRole()
        val address = etUserAddress.text.trim().toString()
        val notes = etUserNotes.text.trim().toString()
        val progressBar = MyProgressBar(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val user = User(name = userName, email = userEmail, role = userRole, address = address, phone = userPhone, notes = notes)
            Users.add(user) { success, error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    progressBar.dismiss()
                    if (success) {
                        SharedViewModelSingleton.userAddedEvent.postValue(user)
                        NotificationUtils.userAdded(user)
                        if (isAddAgain) {
                            CommonUtils.toastMessage(context, "New user ${user.name} has been added. Please add another user.")
                            etUserName.setText("")
                            etUserEmail.setText("")
                            etUserAddress.setText("")
                            etUserPhone.setText("")
                            etUserNotes.setText("")
                        } else {
                            CommonUtils.toastMessage(context, "New user ${user.name} has been added")
                            finish()
                        }
                    } else CommonUtils.showMessage(context, "Not able to add", "Not able to add new user ${user.name}. $error")
                }
            }
        }
    }

    private fun updateUser() {
        val user = user ?: return

        val userName = etUserName.text.trim().toString()
        val userEmail = etUserEmail.text.trim().toString()
        val userPhone = etUserPhone.text.trim().toString()
        if (userName.isEmpty() || (userEmail.isEmpty() && userPhone.isEmpty())) {
            CommonUtils.showMessage(context, " User Details", "Please enter valid user details. Name should not be empty and either email or phone is required.")
            return
        }
        if (userEmail.isNotEmpty() && user.email != userEmail) {
            if (!CommonUtils.isValidEmail(userEmail)) {
                CommonUtils.showMessage(context, "Invalid Email", "Please enter valid email address.")
                return
            }
            if (Users.doesEmailExist(userEmail)) {
                CommonUtils.showMessage(context, "New User Exists", "The new user's email already exists.")
                return
            }
        }
        if (userPhone.isNotEmpty() && user.phone != userPhone) {
            if (Users.doesPhoneExist(userPhone)) {
                CommonUtils.showMessage(context, "New User Exists", "he new user's phone already exists.")
                return
            }
        }
        val userRole = getSelectedRole()
        val address = etUserAddress.text.trim().toString()
        val notes = etUserNotes.text.trim().toString()
        val progressBar = MyProgressBar(this)
        lifecycleScope.launch(Dispatchers.IO) {
            user.name = userName
            user.email = userEmail
            user.role = userRole
            user.address = address
            user.phone = userPhone
            user.notes = notes
            Users.update(user) { success, error ->
                lifecycleScope.launch(Dispatchers.Main)  {
                    progressBar.dismiss()
                    if (success) {
                        SharedViewModelSingleton.userUpdatedEvent.postValue(user)
                        CommonUtils.toastMessage(context, "User ${user.name} has been updated")
                        finish()
                    } else CommonUtils.showMessage(context, "Not able to update", "Not able to update the user ${user.name}. $error")
                }
            }
        }
    }

    private fun deleteUser() {
        UserActions.deleteUser(this, user ?: return) { success, _ ->
            if (success) finish()
        }
    }
}

