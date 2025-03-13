package com.android.hms.model

import com.android.hms.db.UsersDb
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

data class User(var id: String = "", var name: String = "", var role: String = Users.Roles.TENANT.value, var phone: String = "", var email: String = "",
                var address: String = "", var token: String = "", var notes: String = "", var enable: Boolean = true) {

    val roleName get() = when (role) {
            Users.Roles.ADMIN.value -> "Admin"
            Users.Roles.OWNER.value -> "Owner"
            Users.Roles.HELPER.value -> "Helper"
            Users.Roles.TENANT.value -> "Tenant"
            Users.Roles.GUEST.value -> "Guest"
            else -> "Unknown"
        }
}

object Users {

    enum class Roles(val value: String) {
        ADMIN("A"), OWNER("O"), HELPER("H"), TENANT("T"), GUEST("G")
    }

    private var users = Collections.synchronizedList(ArrayList<User>())
    var currentUser: User? = null

    fun isUserLoggedIn(): Boolean {
        return currentUser != null
    }

    fun add(user: User, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        UsersDb.add(user) { success, error -> result(success, error) }
    }

    fun update(user: User, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        UsersDb.update(user) { success, error -> result(success, error) }
    }

    fun delete(user: User, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        UsersDb.delete(user) { success, error -> result(success, error) }
    }

    fun delete(id: String, name: String, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        UsersDb.delete(id, name) { success, error -> result(success, error) }
    }

    fun updateCurrentUserToken(token: String) {
        val user = currentUser ?: return
        if (user.token == token) return
        user.token = token
        update(user)
    }

    fun getById(id: String): User? {
        return getAll().firstOrNull { it.id == id }
    }

    fun getByPhone(phone: String): User? {
        return getAll().firstOrNull { it.phone == phone }
    }

    fun getByEmail(email: String): User? {
        return getAll().firstOrNull { it.email == email }
    }

    fun getByToken(token: String): User? {
        return getAll().firstOrNull { it.token == token }
    }

    fun isCurrentUserAdmin(): Boolean {
        return currentUser?.role == Roles.ADMIN.value
    }

    fun isCurrentUserOwner(): Boolean {
        return currentUser?.role == Roles.OWNER.value
    }

    fun isCurrentUserTenant(): Boolean {
        return currentUser?.role == Roles.TENANT.value
    }

    fun isCurrentUserHelper(): Boolean {
        return currentUser?.role == Roles.HELPER.value
    }

    fun getHelpers(): List<User> {
        return getAll().filter { it.role == Roles.HELPER.value }
    }

    fun getTenants(): List<User> {
        return getAll().filter { it.role == Roles.TENANT.value }
    }

    fun getHelpersFromDb(): ArrayList<User> {
        return UsersDb.getHelpers()
    }

    fun getTenantsFromDb(): ArrayList<User> {
        return UsersDb.getTenants()
    }

    fun doesPhoneNumberExist(phone: String): Boolean {
        return getByPhone(phone) != null // listAll.filter { it.phone == phone }.size
    }

    fun doesUserExist(token: String): Boolean {
        return getByToken(token) != null // listAll.filter { it.phone == token }.size
    }

    fun getAll(): ArrayList<User> {
        if (users.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch { refreshList() }
        }
        return ArrayList(users)
    }

    fun getAllOwners(): ArrayList<User> {
        return ArrayList(getAll().filter { it.role == Roles.OWNER.value }.sortedBy { it.name })
    }

    fun getAllAdmins(): ArrayList<User> {
        return ArrayList(getAll().filter { it.role == Roles.ADMIN.value }.sortedBy { it.name })
    }

    fun getAllTenants(): ArrayList<User> {
        return ArrayList(getAll().filter { it.role == Roles.TENANT.value }.sortedBy { it.name })
    }

    fun getAllHelpers(): ArrayList<User> {
        return ArrayList(getAll().filter { it.role == Roles.HELPER.value }.sortedBy { it.name })
    }

    fun doesPhoneExist(phone: String): Boolean {
        return (users.indexOfFirst { it.phone == phone } != -1)
    }

    fun doesEmailExist(email: String): Boolean {
        return (users.indexOfFirst { it.email == email } != -1)
    }

    fun getAdminsToken(): List<String> {
        return getAllAdmins().map { it.token }
    }

    fun getOwnersToken(): List<String> {
        return getAllOwners().map { it.token }
    }

    fun getOwnersTokenExcept(): List<String> {
        return getAllOwners().map { it.token }.filter { it != currentUser?.token }
    }

    fun refreshList() {
        users = Collections.synchronizedList(UsersDb.getAll())
    }

    fun addLocally(user: User) {
        if (user.id.isEmpty()) return
        if (users.indexOfFirst { it.id == user.id } != -1) return
        users.add(user)
        SharedViewModelSingleton.userAddedEvent.postValue(user)
    }

    fun updateLocally(user: User) {
        if (user.id.isEmpty()) return
        val index = users.indexOfFirst { it.id == user.id }
        if (index == -1) addLocally(user)
        else {
            users[index] = user
            SharedViewModelSingleton.userUpdatedEvent.postValue(user)
        }
    }

    fun deleteLocally(user: User) {
        if (user.id.isEmpty()) return
        val index = users.indexOfFirst { it.id == user.id }
        if (index == -1) return
        users.removeAt(index)
        SharedViewModelSingleton.userRemovedEvent.postValue(user)
    }
}