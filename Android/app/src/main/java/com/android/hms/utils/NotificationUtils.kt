package com.android.hms.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.android.hms.ui.activities.OwnerMainActivity
import com.android.hms.R
import com.android.hms.model.Building
import com.android.hms.model.Expense
import com.android.hms.model.Repair
import com.android.hms.model.Repairs
import com.android.hms.model.House
import com.android.hms.model.Rent
import com.android.hms.model.User
import com.android.hms.model.Users
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationUtils {

    fun userAdded(user: User) {
        postMessage("New user is added","New user ${user.name} has been added as ${user.roleName}")
    }

    fun userRemoved(user: User) {
        userRemoved(user.name, user.roleName)
    }

    fun userRemoved(name: String, role: String) {
        postMessage("User is removed","User $name ($role) has been removed")
    }

    fun buildingAdded(building: Building) {
        postMessage("New building is added","New building ${building.name} has been added and it's address is ${building.address}")
    }

    fun buildingRemoved(building: Building) {
        postMessage("Building is removed","Building ${building.name} has been removed")
    }

    fun houseAdded(house: House) {
        postMessage("New house is added","New house ${house.name} has been added in ${house.bName} and it's deposit is ${CommonUtils.formatNumToText(house.deposit)} and rent is ${CommonUtils.formatNumToText(house.rent)}")
    }

    fun houseRented(house: House) {
        postMessage("House rented","House ${house.name} has been rented to ${house.tName} with the deposit of ${CommonUtils.formatNumToText(house.deposit)} and rent as ${CommonUtils.formatNumToText(house.rent)} from ${CommonUtils.getFullDayDateFormatText(house.tJoined)} and notes: ${house.notes}")
    }

    fun rentPaid(rent: Rent) {
        postMessage("Rent paid","Rent of $${CommonUtils.formatNumToText(rent.amount)} has been paid by ${rent.tName} for the house ${rent.hName} on ${CommonUtils.getFullDayDateFormatText(rent.paidOn)} with delay of ${rent.delay} days and notes: ${rent.notes}")
    }

    fun rentRemoved(rent: Rent) {
        postMessage("Rent paid is removed","Rent of $${CommonUtils.formatNumToText(rent.amount)} has been paid for the house ${rent.hName} is removed and notes: ${rent.notes}")
    }

    fun expenseSubmitted(expense: Expense) {
        postMessage("Expense paid","Expense of $${CommonUtils.formatNumToText(expense.amount)} has been paid by ${expense.paidBy} for ${expense.hName.ifEmpty { expense.bName }} and notes: ${expense.notes} on ${CommonUtils.getFullDayDateFormatText(expense.paidOn)}")
    }

    fun expenseUpdated(expense: Expense) {
        postMessage("Expense updated","Expense of $${CommonUtils.formatNumToText(expense.amount)} has been updated for ${expense.hName.ifEmpty { expense.bName }} and notes: ${expense.notes} on ${CommonUtils.getFullDayDateFormatText(expense.paidOn)}")
    }

    fun expenseRemoved(expense: Expense) {
        postMessage("Expense paid is removed","Expense of $${CommonUtils.formatNumToText(expense.amount)} has been paid for ${expense.hName.ifEmpty { expense.bName }}is removed and notes: ${expense.notes}")
    }

    fun houseVacated(house: House) {
        postMessage("House vacated","House ${house.name} has been vacated from ${CommonUtils.getFullDayDateFormatText(house.tLeft)} and notes: ${house.notes}")
    }

    fun houseRemoved(house: House) {
        postMessage("House is removed","House ${house.name} has been removed from ${house.bName}")
    }

    fun repairSubmitted(repair: Repair) { if (repair.bId.isEmpty()) houseRepairSubmitted(repair) else buildingRepairSubmitted(repair) }

    private fun buildingRepairSubmitted(repair: Repair) {
        if (repair.amount > 0)
            postMessage("Building repair is submitted","New repair ${repair.desc} has been submitted for building ${repair.bName} with the amount of $${CommonUtils.formatNumToText(repair.amount)} on ${CommonUtils.getFullDayDateFormatText(repair.raisedOn)} with notes: ${repair.notes} and status is ${repair.status}")
        else
            postMessage("Building repair is submitted","New repair ${repair.desc} has been submitted for building ${repair.bName} on ${CommonUtils.getFullDayDateFormatText(repair.raisedOn)} with notes: ${repair.notes} and status is ${repair.status}")
    }

    private fun houseRepairSubmitted(repair: Repair) {
        if (repair.amount > 0)
            postMessage("House repair is submitted","New repair ${repair.desc} has been submitted for house ${repair.hName} with the amount of $${CommonUtils.formatNumToText(repair.amount)} on ${CommonUtils.getFullDayDateFormatText(repair.raisedOn)} with notes: ${repair.notes} and status is ${repair.status}")
        else
            postMessage("House repair is submitted","New repair ${repair.desc} has been submitted for house ${repair.hName} on ${CommonUtils.getFullDayDateFormatText(repair.raisedOn)} with notes: ${repair.notes} and status is ${repair.status}")
    }

    fun repairUpdated(repair: Repair) {
        when (repair.status) {
            Repairs.statuses[2] -> repairBlocked(repair)
            Repairs.statuses[3] -> repairFixed(repair)
            Repairs.statuses[4] -> repairPaid(repair)
            Repairs.statuses[5] -> repairClosed(repair)
            else -> if (repair.hId.isEmpty()) postMessage("Building repair is updated","Repair ${repair.desc} has been updated for building ${repair.bName} with notes: ${repair.notes}")
            else postMessage("House repair is updated","Repair ${repair.desc} has been updated for house ${repair.hName} with notes: ${repair.notes}")
        }
    }

    private fun repairBlocked(repair: Repair) {
        if (repair.hId.isEmpty())
            postMessage("Building repair is blocked","Repair ${repair.desc} has been marked as blocked for building ${repair.bName} with notes: ${repair.notes}")
        else
            postMessage("House repair is blocked","Repair ${repair.desc} has been marked as blocked for house ${repair.hName} with notes: ${repair.notes}")
    }

    private fun repairFixed(repair: Repair) {
        if (repair.hId.isEmpty())
            postMessage("Building repair is fixed","Repair ${repair.desc} has been fixed for building ${repair.bName} on ${CommonUtils.getFullDayDateFormatText(repair.fixedOn)} with notes: ${repair.notes}")
        else
            postMessage("House repair is fixed","Repair ${repair.desc} has been fixed for house ${repair.hName} on ${CommonUtils.getFullDayDateFormatText(repair.fixedOn)} with notes: ${repair.notes}")
    }

    private fun repairPaid(repair: Repair) {
        if (repair.hId.isEmpty())
            postMessage("Building repair is paid","Repair ${repair.desc} has been paid for building ${repair.bName} with the amount of $${CommonUtils.formatNumToText(repair.amount)} on ${CommonUtils.getFullDayDateFormatText(repair.paidOn)} with notes: ${repair.notes}")
        else
            postMessage("House repair is paid","Repair ${repair.desc} has been paid for house ${repair.hName} with the amount of $${CommonUtils.formatNumToText(repair.amount)} on ${CommonUtils.getFullDayDateFormatText(repair.paidOn)} with notes: ${repair.notes}")
    }

    fun repairClosed(repair: Repair) {
        if (repair.hId.isEmpty())
            postMessage("Building repair is closed","Repair ${repair.desc} has been closed for building ${repair.bName} with notes: ${repair.notes}")
        else
            postMessage("House repair is closed","Repair ${repair.desc} has been closed for house ${repair.hName} with notes: ${repair.notes}")
    }

    fun repairRemoved(repair: Repair) {
        if (repair.hId.isEmpty())
            postMessage("Building repair is removed","Repair ${repair.desc} has been removed for building ${repair.bName} with notes: ${repair.notes}")
        else
            postMessage("House repair is removed","Repair ${repair.desc} has been removed for house ${repair.hName} with notes: ${repair.notes}")
    }

    private fun postMessage(title: String, message: String) {
        val tokenList = Users.getOwnersToken()
        FCMNotification.send(title, message, tokenList)
    }

    fun show(context: Context, title:String, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val channelId = "${context.packageName}-${context.getString(R.string.app_name)}-101"
            val channelID = "com.android.hms.101"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(channelID, channelId, NotificationManager.IMPORTANCE_DEFAULT)
                notificationChannel.description = message
                notificationManager.createNotificationChannel(notificationChannel)
            }
            val resultIntent = Intent(context, OwnerMainActivity::class.java)
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_menu_gallery)
                .setChannelId(channelID)
                .setContentIntent(pendingIntent)

            notificationManager.notify(0, notificationBuilder.build())
            // notificationManager.notify(CommonUtils.currentTime.toInt(), notificationBuilder.build())
        }
    }
}