package com.android.hms.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.User
import com.android.hms.model.Users
import com.android.hms.ui.UserActions
import com.android.hms.ui.adapters.UserAdapter
import com.android.hms.utils.Globals
import com.android.hms.viewmodel.SharedViewModelSingleton
import com.google.android.material.floatingactionbutton.FloatingActionButton 
import kotlinx.coroutines.launch

class ManageUsersActivity : BaseActivity() {

    private lateinit var tvOwners: TextView
    private lateinit var tvHelpers: TextView
    private lateinit var tvTenants: TextView

    private lateinit var recyclerViewOwners: RecyclerView
    private lateinit var recyclerViewHelpers: RecyclerView
    private lateinit var recyclerViewTenants: RecyclerView

    private var ownerAdapter: UserAdapter? = null
    private var helperAdapter: UserAdapter? = null
    private var tenantsAdapter: UserAdapter? = null

    private var ownersList = ArrayList<User>()
    private var helpersList = ArrayList<User>()
    private var tenantsList = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        setActionBarView("Manage Users")
        val jobO = lifecycleScope.launch { initOwners() }
        val jobH = lifecycleScope.launch { initHelpers() }
        val jobT = lifecycleScope.launch { initTenants() }
        lifecycleScope.launch {
            jobO.join()
            jobH.join()
            jobT.join()
            initializeObservers()
        }

        findViewById<FloatingActionButton>(R.id.fabAddUser).setOnClickListener {
            startActivity(Intent(this, AddUserActivity::class.java))
        }
    }

    override fun filter(searchText: String) {
        ownerAdapter?.filter(searchText)
        helperAdapter?.filter(searchText)
        tenantsAdapter?.filter(searchText)
    }

    private fun initOwners() {
        tvOwners = findViewById(R.id.tvOwners)
        recyclerViewOwners = findViewById(R.id.recyclerViewOwners)
        recyclerViewOwners.layoutManager = LinearLayoutManager(this)
        ownersList = Users.getAllOwners()
        tvOwners.text = "Owners: ${ownersList.size}"
        ownerAdapter = UserAdapter(ownersList, ::editUser, ::deleteUser)
        recyclerViewOwners.adapter = ownerAdapter
        recyclerViewOwners.isNestedScrollingEnabled = false
    }

    private fun initHelpers() {
        tvHelpers = findViewById(R.id.tvHelpers)
        recyclerViewHelpers = findViewById(R.id.recyclerViewHelpers)
        recyclerViewHelpers.layoutManager = LinearLayoutManager(this)
        helpersList = Users.getAllHelpers()
        tvHelpers.text = if (helpersList.isEmpty()) "No Helpers found. Please add a Helper."
        else "Helpers: ${helpersList.size}"
        helperAdapter = UserAdapter(helpersList, ::editUser, ::deleteUser)
        recyclerViewHelpers.adapter = helperAdapter
        recyclerViewHelpers.isNestedScrollingEnabled = false
    }

    private fun initTenants() {
        tvTenants = findViewById(R.id.tvTenants)
        recyclerViewTenants = findViewById(R.id.recyclerViewTenants)
        recyclerViewTenants.layoutManager = LinearLayoutManager(this)
        tenantsList = Users.getAllTenants()
        tvTenants.text = if (tenantsList.isEmpty()) "No Tenants found. Please add a Tenant."
        else "Tenants: ${tenantsList.size}"
        tenantsAdapter = UserAdapter(tenantsList, ::editUser, ::deleteUser)
        recyclerViewTenants.adapter = tenantsAdapter
        recyclerViewTenants.isNestedScrollingEnabled = false
    }

    private fun initializeObservers() {
        SharedViewModelSingleton.userAddedEvent.observe(this) { user -> updateAdapterForUserAddition(user) }
        SharedViewModelSingleton.userUpdatedEvent.observe(this) { user -> updateAdapterForUserChange(user) }
        SharedViewModelSingleton.userRemovedEvent.observe(this) { user -> updateAdapterForUserRemove(user) }
    }

    private fun editUser(user: User) {
        val intent = Intent(this, AddUserActivity::class.java)
        intent.putExtra(Globals.gFieldId, user.id)
        startActivity(intent)
    }

    private fun deleteUser(user: User) {
        UserActions.deleteUser(this, user) { success, _ ->
            if (success) updateAdapterForUserRemove(user)
        }
    }

    private fun updateAdapterForUserAddition(user: User) {
        when (user.role) {
            Users.Roles.OWNER.value -> {
                updateAdapterForUserAddition(user, ownersList, ownerAdapter ?: return)
                tvOwners.text = "Owners: ${ownersList.size}"
            }
            Users.Roles.HELPER.value -> {
                updateAdapterForUserAddition(user, helpersList, helperAdapter ?: return)
                tvHelpers.text = "Helpers: ${helpersList.size}"
            }
            Users.Roles.TENANT.value -> {
                updateAdapterForUserAddition(user, tenantsList, tenantsAdapter ?: return)
                tvTenants.text = "Tenants: ${tenantsList.size}"
            }
        }
    }

    private fun updateAdapterForUserChange(user: User) {
        when (user.role) {
            Users.Roles.OWNER.value -> updateAdapterForUserChange(user, ownersList, ownerAdapter ?: return)
            Users.Roles.HELPER.value -> updateAdapterForUserChange(user, helpersList, helperAdapter ?: return)
            Users.Roles.TENANT.value -> updateAdapterForUserChange(user, tenantsList, tenantsAdapter ?: return)
        }
    }

    private fun updateAdapterForUserRemove(user: User) {
        when (user.role) {
            Users.Roles.OWNER.value -> {
                updateAdapterForUserRemove(user, ownersList, ownerAdapter ?: return)
                tvOwners.text = "Owners: ${ownersList.size}"
            }
            Users.Roles.HELPER.value -> {
                updateAdapterForUserRemove(user, helpersList, helperAdapter ?: return)
                tvHelpers.text = "Helpers: ${helpersList.size}"
            }
            Users.Roles.TENANT.value -> {
                updateAdapterForUserRemove(user, tenantsList, tenantsAdapter ?: return)
                tvTenants.text = "Tenants: ${tenantsList.size}"
            }
        }
    }

    private fun updateAdapterForUserAddition(user: User, userList: ArrayList<User>, userAdapter: UserAdapter) {
        if (userList.indexOfFirst { it.id == user.id } != -1) return
        userList.add(user)
        userAdapter.notifyItemInserted(userList.size) // header row is included
    }

    private fun updateAdapterForUserChange(user: User, userList: ArrayList<User>, userAdapter: UserAdapter) {
        val index = userList.indexOfFirst { it.id == user.id }
        if (index == -1) {
            updateAdapterForUserAddition(user, userList, userAdapter)
            return
        }
        userList[index] = user
        userAdapter.notifyItemChanged(index + 1) // header row is included
    }

    private fun updateAdapterForUserRemove(user: User, userList: ArrayList<User>, userAdapter: UserAdapter) {
        val index = userList.indexOfFirst { it.id == user.id }
        if (index == -1) return
        userList.removeAt(index)
        userAdapter.notifyItemRemoved(index + 1) // header row is included
    }
}
