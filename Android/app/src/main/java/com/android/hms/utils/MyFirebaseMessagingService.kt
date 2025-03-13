package com.android.hms.utils

import android.util.Log
import com.android.hms.model.Users
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MyFirebaseMessagingService : FirebaseMessagingService(), CoroutineScope {

    private val masterJob = Job()
    override val coroutineContext: CoroutineContext get() = Dispatchers.IO + masterJob

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        super.onMessageReceived(remoteMessage)

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        // "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        /* remoteMessage.data.isNotEmpty().let {
            // "Message data payload: " + remoteMessage.data)

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob()
            } else {
                // Handle message within 10 seconds
                handleNow()
            }
        } */

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            launch(Dispatchers.Main) {
                // CommonUtils.toastMessage(applicationContext, "${ it.title }\n${ it.body }")
                // CommonUtils.showMessage(applicationContext, it.title!!, it.body!!)
                NotificationUtils.show(applicationContext, it.title ?: return@launch, it.body ?: return@launch)
            }
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        // You can also access data payload here
        remoteMessage.data.let {
            Log.d("FCM", "Message Data Payload: $it")
        }

        // Optionally, display a notification
        // showNotification(remoteMessage)
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        // If you want to send messages to this application instance or manage this apps subscriptions on the server side, send the Instance ID token to your app server.
        // sendRegistrationToServer(token)
        super.onNewToken(token)

        UserPreferences(applicationContext).token = token
        Users.updateCurrentUserToken(token)
    }
}

