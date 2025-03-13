package com.android.hms.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.hms.model.Building
import com.android.hms.model.Buildings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuildingViewModel : ViewModel() {
    private val selectedBuildingEvent = MutableLiveData<Building>()
}