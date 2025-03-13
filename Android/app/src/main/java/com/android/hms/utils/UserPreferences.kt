package com.android.hms.utils

import android.content.Context

// for saving preferences
class UserPreferences(context: Context) {

    val sharedPreferences get() = preferences!!

    private val preferences = context.getSharedPreferences("HMSUserPreferences", Context.MODE_PRIVATE)

    // private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    // private val preferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

    var name = preferences.getString(Globals.gDefaultsName, "")!!
        set(value) = preferences.edit().putString(Globals.gDefaultsName, value).apply()

    var email = preferences.getString(Globals.gUserDefaultsEmail, "")!!
        set(value) = preferences.edit().putString(Globals.gUserDefaultsEmail, value).apply()

    var token = preferences.getString(Globals.gUserDefaultsToken, "")!!
        set(value) = preferences.edit().putString(Globals.gUserDefaultsToken, value).apply()

    var phone = preferences.getString(Globals.gUserDefaultsPhone, "")!!
        set(value) = preferences.edit().putString(Globals.gUserDefaultsPhone, value).apply()

    //   val allNotifications = preferences.getBoolean(context.getString(R.string.all_notifications), true)

    fun removeAll() {
        preferences.edit().clear().apply()
    }
}

