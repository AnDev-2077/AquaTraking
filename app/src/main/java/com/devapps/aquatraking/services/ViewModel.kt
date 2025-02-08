package com.devapps.aquatraking.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ViewModel: ViewModel() {
    private val _selectedKey = MutableLiveData<String?>()
    val selectedKey: LiveData<String?> = _selectedKey

    private val _availableKeys = MutableLiveData<List<String>>(emptyList())
    val availableKeys: LiveData<List<String>> = _availableKeys

    fun setSelectedKey(key: String) {
        _selectedKey.value = key
    }

    fun setAvailableKeys(keys: List<String>) {
        _availableKeys.value = keys
    }
}