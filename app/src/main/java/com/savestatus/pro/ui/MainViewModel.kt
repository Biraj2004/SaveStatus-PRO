package com.savestatus.pro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.savestatus.pro.data.StatusRepository
import com.savestatus.pro.model.StatusItem
import com.savestatus.pro.model.StatusType
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatusRepository(application.applicationContext)

    private val _isBusinessMode = MutableLiveData(false)
    val isBusinessMode: LiveData<Boolean> = _isBusinessMode

    private val _allStatuses = MutableLiveData<List<StatusItem>>(emptyList())
    val allStatuses: LiveData<List<StatusItem>> = _allStatuses

    private val _downloadedStatuses = MutableLiveData<List<StatusItem>>(emptyList())
    val downloadedStatuses: LiveData<List<StatusItem>> = _downloadedStatuses

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    val imageStatuses: LiveData<List<StatusItem>> = _allStatuses.map { list ->
        list.filter { it.type == StatusType.IMAGE }
    }

    val videoStatuses: LiveData<List<StatusItem>> = _allStatuses.map { list ->
        list.filter { it.type == StatusType.VIDEO }
    }

    fun toggleMode(isBusiness: Boolean) {
        if (_isBusinessMode.value != isBusiness) {
            _isBusinessMode.value = isBusiness
            loadStatuses()
        }
    }

    fun loadStatuses() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val isBusiness = _isBusinessMode.value ?: false
                val statuses = repository.loadStatuses(isBusiness)
                _allStatuses.value = statuses
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load statuses: ${e.message}"
                _allStatuses.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDownloadedStatuses() {
        viewModelScope.launch {
            try {
                val downloaded = repository.getDownloadedStatuses()
                _downloadedStatuses.value = downloaded
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadedStatuses.value = emptyList()
            }
        }
    }

    /**
     * Downloads [item] and returns true on success, false on failure.
     * Automatically refreshes the Downloaded list on success.
     * Designed to be called from a fragment's lifecycleScope so each tab
     * handles its own result independently — no cross-fragment observer pollution.
     */
    suspend fun downloadStatus(item: StatusItem): Boolean {
        return try {
            val success = repository.downloadStatus(item)   // already runs on IO
            if (success) loadDownloadedStatuses()
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    init {
        loadDownloadedStatuses()
    }
}

