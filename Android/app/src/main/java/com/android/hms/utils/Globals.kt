package com.android.hms.utils

import com.android.hms.db.Connection
import com.android.hms.model.Buildings
import com.android.hms.model.ExpenseCategories
import com.android.hms.model.Repairs
import com.android.hms.model.Houses
import com.android.hms.model.Rents
import com.android.hms.model.RepairDescriptions
import com.android.hms.model.Users
import com.android.hms.viewmodel.SharedViewModelSingleton
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Created by SivaMalini on 24-02-2018.
 */

/* http://www.appsdeveloperblog.com/push-notifications-example-kotlin-firebase
 https://inthecheesefactory.com/blog/get-to-know-glide-recommended-by-google/en
https://github.com/rs/SDWebImage
https://github.com/onevcat/Kingfisher

https://pusher.com/tutorials/chat-kotlin-android
 */


object Globals { // singleton
    // global constants should be declared here

    const val gDevelopmentMode = true

    const val gMinNameChars = 4
    const val gAdminName = "admin"
    const val gMe = "me"
    const val gFullDateTimeFormatWithSecondsForFile = "yyyy MMM dd - h mm ss a"
    const val gFullDayDateTimeFormat = "EEE d MMM yyyy - h:mm:ss a"
    const val gFullDateTimeFormat = "dd MMM yyyy h:mm a"
    const val gFullDayDateFormat = "EEE d MMM yy"
    const val gDayMonthFormat = "EEE d MMM"
    const val gShortDayMonthFormat = "d MMM"
    const val gShortDayMonthYearFormat = "d MMM yy"
    const val gMonthYearOnlyFormat = "MMM yy"
    const val gTimeFormat = "h:mm a"

    val gAdminPhones = listOf("+919008110511","+919448975141", "+911234567890", "+911234567891")

    // NOTE: gAppDir and gFolderNameChars ARE HARD CODED IN SERVER SIDE AS WELL
    const val gAppDir = "app" // saves all documents, images, audios and videos
    const val gFolderNameChars = 4
    const val gMaxFileSizeInMB = 64
    const val gMaxPhotoSizeInMB = 16

    const val nHomeLoginNameChars = 10
    const val allText = "[ALL]"
    var serverURL = "http://localhost:9000"
    var fileServerURL = "" // "http://localhost:9000"
    const val gLastSeenTimeDifference = 60 // minutes

    const val minFieldLength = 5
    const val rentThreshold = 5 // in days

    const val gFieldId = "id"
    const val gFieldHouseId = "hId"
    const val gFieldBuildingId = "bId"
    const val gFieldTenantId = "tId"
    const val gFieldBuildingName = "bName"
    const val gFieldTenantName = "tName"
    const val gFieldHouseName = "hName"

    const val gDefaultsName = "name"
    const val gUserDefaultsEmail = "email"
    const val gUserDefaultsToken = "token"
    const val gUserDefaultsPhone = "phone"

    const val gZeroInText = "-  "
    const val gNetworkErrorCode = 10001

    const val gPhotoExt = ".jpg"
    const val gImageExt = ".png" // gVideoExt = ".mp4", gAudioExt = ".m4a"
    const val gThumbnail = "t"
    const val gTickMark = "\u2713"

    const val gPhotoThumbnailHeight = 100
    const val gPhotoThumbnailWidth = 100
    const val gImageThumbnailHeight = 150
    const val gImageThumbnailWidth = 450
    const val gDefaultTheme = "Medium Blue"

    const val gSyncTimeIntervalInMinutes: Long = 600 // in minutes

    const val gFaqUrl = "https://www.mywebsite.com/faq"

    const val gLoadSampleDataIfEmpty = true

    suspend fun initialize() {
        try {
            // FirebaseApp.initializeApp(context)
            val settings = firestoreSettings {
                setLocalCacheSettings(memoryCacheSettings {}) // Use memory cache
                setLocalCacheSettings(persistentCacheSettings {}) // Use persistent disk cache (default)
            }
            Connection.db.firestoreSettings = settings
        } catch (e: Exception) {
            CommonUtils.printException(e)
        }

        refreshLists()
    }

    private var initInProgress = false
    private val mutex = Mutex()
    private suspend fun refreshLists() {
        mutex.withLock { // Acquire the lock
            if (initInProgress) return
            Users.refreshList() // start this immediately
            val scope = CoroutineScope(Dispatchers.IO)
            val jobBuilding = scope.launch { Buildings.refreshList() } // start this immediately
            val jobHouse = scope.launch { Houses.refreshList() } // start this immediately
            val jobRepair = scope.launch { Repairs.refreshList() } // start this immediately
            val jobRent = scope.launch { Rents.refreshList() } // start this immediately
            scope.launch { ExpenseCategories.refreshList() }
            scope.launch { RepairDescriptions.refreshList() }
            scope.launch { Connection.setObservers() }
            scope.launch {
                jobBuilding.join()
                jobHouse.join()
                jobRepair.join()
                jobRent.join()
                SharedViewModelSingleton.refreshListsEvent.postValue(true)
                initInProgress = false
            }
        }
    }
}
