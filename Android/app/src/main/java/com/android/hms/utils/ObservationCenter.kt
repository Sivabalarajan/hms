package com.android.hms.utils

object ObservationCenter {
    private val registeredObjects = HashMap<String, Runnable>()

    @Synchronized
    fun setFunction(observationName: String, runnable: Runnable) {
        registeredObjects[observationName] = runnable
    }

    @Synchronized
    fun removeFunction(observationName: String) {
        registeredObjects.remove(observationName)
    }

    @Synchronized
    fun postObservation(observationName: String) {
        val runnable = registeredObjects[observationName] ?: return
        runnable.run()
    }

    const val userListChanged = "UserListChanged"
}

/*
object ObservationCenter {
    private val registeredObjects = HashMap<String, ArrayList<Runnable>>()

    @Synchronized
    fun addFunction(observationName: String, runnable: Runnable) {
        if (registeredObjects[observationName] == null) registeredObjects[observationName] = ArrayList()
        registeredObjects[observationName]?.add(runnable)
    }

    @Synchronized
    fun removeFunction(observationName: String, runnable: Runnable) {
        registeredObjects[observationName]?.remove(runnable)
    }

    @Synchronized
    fun removeAllFunctions(observationName: String) {
        registeredObjects[observationName]?.clear()
    }

    @Synchronized
    fun post(observationName: String) {
        val runnableList = registeredObjects[observationName] ?: return
        for (runnable in runnableList) runnable.run()
    }

    object ObservationName {

    }
}
 */