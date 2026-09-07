package com.ritesh.cashiro.data.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsScanManager @Inject constructor() {
    fun startSmsLoggingScan() {}
    fun cancelSmsScanning() {}
    fun getSmsScanWorkInfo(): LiveData<List<WorkInfo>> = MutableLiveData(emptyList())
}
