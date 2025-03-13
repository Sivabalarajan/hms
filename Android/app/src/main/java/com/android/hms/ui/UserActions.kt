package com.android.hms.ui

import android.content.Context
import android.content.DialogInterface
import com.android.hms.model.Houses
import com.android.hms.model.User
import com.android.hms.model.Users
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.NotificationUtils
import com.android.hms.viewmodel.SharedViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object UserActions {

    fun deleteUser(context: Context, user: User, result: (success: Boolean, error: String) -> Unit = { _, _ -> }) {
        if (user.id == Users.currentUser?.id) {
            CommonUtils.showMessage(context, "You can't remove yourself", "You will not be able to remove yourself. Please make someone as Owner and request the new Owner to remove you.")
            result(false, "You can't remove yourself")
            return
        }

        val alertDialog = CommonUtils.confirmMessage(context, "Remove User", "Are you sure you want to remove this '${user.name}' user? Please confirm.", "Remove User")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progressBar = MyProgressBar(context, "Removing the user details. Please wait...")
            CoroutineScope(Dispatchers.Main).launch {
                if (user.role == Users.Roles.TENANT.value && Houses.getTenantHouses(user.id).isNotEmpty()) {
                    CommonUtils.showMessage(context, "Tenant associated with house(s)", "The ${user.name} is associated with one or more houses. Please de-associate from other houses before removing from application.")
                    progressBar.dismiss()
                    result(false, "Tenant associated with house(s)")
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    Users.delete(user) { success, error ->
                        CoroutineScope(Dispatchers.Main).launch {
                            progressBar.dismiss()
                            if (success) {
                                SharedViewModelSingleton.userRemovedEvent.postValue(user)
                                NotificationUtils.userRemoved(user)
                                CommonUtils.toastMessage(context, "User ${user.name} has been removed")
                            } else CommonUtils.showMessage(context, "Not able to remove", "Not able to remove the user ${user.name}. $error")
                            alertDialog.dismiss()
                            result(success, error)
                        }
                    }
                }
            }
        }
    }
}
