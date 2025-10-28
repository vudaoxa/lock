package com.toh.usagestat.screen.app_detail

import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel

class AppDetailViewModel(
    private val packageName: String,
    private val usageStatsManager: UsageStatsManager,
    private val packageManager: PackageManager
) : ViewModel() {

    //class Factory(private val packageName: String) : ViewModelProvider.Factory {
    //    override fun <T : ViewModel> create(modelClass: Class<T>): T {
    //        return AppDetailViewModel(
    //            packageName,
    //            // Inject dependencies here
    //        ) as T
    //    }
    //}
    //
    //private val _appInfo = MutableLiveData<AppDetailInfo>()
    //val appInfo: LiveData<AppDetailInfo> = _appInfo
    //
    //init {
    //    loadAppDetail()
    //}
    //
    //private fun loadAppDetail() {
    //    viewModelScope.launch {
    //        _appInfo.value = getAppDetailInfo(packageName)
    //    }
    //}
}