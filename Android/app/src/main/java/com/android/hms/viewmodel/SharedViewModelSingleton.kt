package com.android.hms.viewmodel

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.hms.model.Building
import com.android.hms.model.Expense
import com.android.hms.model.Repair
import com.android.hms.model.House
import com.android.hms.model.Rent
import com.android.hms.model.User
import java.util.concurrent.atomic.AtomicBoolean

object SharedViewModelSingleton {

    val refreshListsEvent = SingleLiveEvent<Boolean>()
    // fun refreshLists(refreshed: Boolean) { refreshListsEvent.value = refreshed }

    val selectedBuildingEvent = SingleLiveEvent<Building>()
    // val selectedHouseEvent = MutableLiveData<House>()
    var currentExpenseObject: Expense? = null
    var currentRepairObject: Repair? = null
    var currentHouseObject: House? = null

    val userAddedEvent = SingleLiveEvent<User>()
    val userUpdatedEvent = SingleLiveEvent<User>()
    val userRemovedEvent = SingleLiveEvent<User>()

    val houseAddedEvent = SingleLiveEvent<House>()
    val houseUpdatedEvent = SingleLiveEvent<House>()
    val houseRemovedEvent = SingleLiveEvent<House>()

    val buildingAddedEvent = SingleLiveEvent<Building>()
    val buildingUpdatedEvent = SingleLiveEvent<Building>()
    val buildingRemovedEvent = SingleLiveEvent<Building>()

    val rentPaidEvent = SingleLiveEvent<Rent>()
    val rentUpdatedEvent = SingleLiveEvent<Rent>()
    val rentRemovedEvent = SingleLiveEvent<Rent>()

    val repairInitiatedEvent = SingleLiveEvent<Repair>()
    val repairUpdatedEvent = SingleLiveEvent<Repair>()
    val repairRemovedEvent = SingleLiveEvent<Repair>()

    val expenseSubmittedEvent = SingleLiveEvent<Expense>()
    val expenseUpdatedEvent = SingleLiveEvent<Expense>()
    val expenseRemovedEvent = SingleLiveEvent<Expense>()

    /* fun houseAdded(house: House) { houseAddedEvent.value = house }
    fun houseUpdated(house: House) { houseUpdatedEvent.value = house }
    fun houseDeleted(house: House) { houseDeletedEvent.value = house }

    fun buildingAdded(building: Building) { buildingAddedEvent.value = building }
    fun buildingUpdated(building: Building) { buildingUpdatedEvent.value = building }
    fun buildingDeleted(building: Building) { buildingDeletedEvent.value = building }

    fun rentPaid(rent: Rent) { rentPaidEvent.value = rent }
    fun rentDeleted(rent: Rent) { rentDeletedEvent.value = rent }

    fun repairSubmitted(repair: Repair) { repairSubmittedEvent.value = repair }
    fun repairUpdated(repair: Repair) { repairUpdatedEvent.value = repair }
    fun repairDeleted(repair: Repair) { repairDeletedEvent.value = repair } */

    class SingleLiveEvent<T> : MutableLiveData<T>() {
        private val pending = AtomicBoolean(false)

        @MainThread
        override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
            if (hasActiveObservers()) {
                Log.w("SingleLiveEvent", "Multiple observers registered but only one will be notified of changes.")
            }

            // Observe the internal MutableLiveData
            super.observe(owner) { t ->
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(t)
                }
            }
        }

        @MainThread
        override fun setValue(t: T?) {
            pending.set(true)
            super.setValue(t)
        }

        /**
         * Used for cases where T is Void, to make calls cleaner.
         */
        @MainThread
        fun call() {
            setValue(null)
        }
    }
}
