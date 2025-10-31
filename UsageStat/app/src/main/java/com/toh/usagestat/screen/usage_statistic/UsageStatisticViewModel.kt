//package com.toh.usagestat.screen.usage_statistic
//
//import android.app.usage.UsageEvents
//import android.app.usage.UsageStatsManager
//import android.app.usage.UsageStatsManager.INTERVAL_DAILY
//import android.content.Intent
//import android.content.pm.ApplicationInfo
//import android.content.pm.PackageManager
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.toh.usagestat.screen.usage_statistic.adapter.AppUsageData
//import com.toh.usagestat.screen.usage_statistic.date.DateHeaderItem
//import com.toh.usagestat.screen.usage_statistic.model.UsageStatisticUiState
//import com.toh.usagestat.util.formatDuration
//import com.toh.usagestat.util.getOffsetDate
//import com.toh.usagestat.util.isAfter
//import com.toh.usagestat.util.isBefore
//import com.toh.usagestat.util.isSameDay
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Locale
//import javax.inject.Inject

// //1st version
//@HiltViewModel
//class UsageStatisticViewModel @Inject constructor(
//    private val usageStatsManager: UsageStatsManager,
//    private val packageManager: PackageManager,
//    //private val workManager: WorkManager
//) : ViewModel() {
//
//    private val _uiState = MutableLiveData<UsageStatisticUiState>()
//    val uiState: LiveData<UsageStatisticUiState> = _uiState
//
//    private val _dateList = MutableLiveData<List<DateHeaderItem>>()
//    val dateList: LiveData<List<DateHeaderItem>> = _dateList
//
//    private var currentStartWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
//    private var selectedDate = Calendar.getInstance()
//    //var selectedDate = Calendar.getInstance()
//    //    private set
//
//    init {
//        loadInitialWeek()
//        //loadInitialDate()
//    }
//
//    //fun smoothScrollToDate(): Int {
//    //    return dateList.value.map { it.date }.indexOf(selectedDate)
//    //}
//
//    fun loadInitialDate() {
//        _uiState.value = UsageStatisticUiState(
//            //date = _dateFormat.format(Calendar.getInstance().time),
//            date = _dateFormat.format(selectedDate.time),
//        )
//    }
//
//    fun loadInitialData() {
//        loadDataForDate(selectedDate)
//        startRealTimeTracking() // BẮT ĐẦU THEO DÕI
//    }
//
//    fun selectDate(date: Calendar) {
//        selectedDate = date
//        updateDateSelection()
//        loadDataForDate(date)
//    }
//
//    fun loadPreviousWeek() {
//        currentStartWeek.add(Calendar.DAY_OF_YEAR, -7)
//        loadWeek(currentStartWeek, addToFront = true)
//    }
//
//    fun loadNextWeek() {
//        currentStartWeek.add(Calendar.DAY_OF_YEAR, 7)
//        loadWeek(currentStartWeek, addToFront = false)
//    }
//
//    fun selectDateOffset(offset: Int) {
//        if ((offset < 0 && selectedDate.isAfter(_dateList.value.first().date))
//            || offset > 0 && selectedDate.isBefore(_dateList.value.last().date)
//        ) {
//            selectDate(selectedDate.getOffsetDate(offset))
//        }
//    }
//
//    fun selectLastDate(): Boolean {
//        selectDate(_dateList.value.last().date)
//        return true
//    }
//
//    fun selectFirstDate(): Boolean {
//        selectDate(_dateList.value.first().date)
//        return true
//    }
//
//    private fun loadInitialWeek() {
//        currentStartWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
//        loadWeek(currentStartWeek, addToFront = false)
//    }
//
//    private fun loadWeek(start: Calendar, addToFront: Boolean) {
//        viewModelScope.launch {
//            val newWeek = mutableListOf<DateHeaderItem>()
//            val tempStart = start.clone() as Calendar
//            for (i in 0..6) {
//                val date = tempStart.clone() as Calendar
//                date.add(Calendar.DAY_OF_YEAR, i)
//                newWeek.add(DateHeaderItem(date, isSameDay(date, selectedDate)))
//            }
//
//            val current = _dateList.value?.toMutableList() ?: mutableListOf()
//            if (addToFront) {
//                current.addAll(0, newWeek)
//            } else {
//                current.addAll(newWeek)
//            }
//
//            // Giữ 35 ngày (5 tuần)
//            val trimmed = if (current.size > 35) current.takeLast(35) else current
//            _dateList.value = trimmed
//            //_dateList.value = current
//        }
//    }
//
//    private var seekJob: Job? = null
//
//    // Hàm lọc package hệ thống
//    private fun isSystemPackage(packageName: String): Boolean {
//        return try {
//            val appInfo = packageManager.getApplicationInfo(packageName, 0)
//            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
//        } catch (e: Exception) {
//            true // Nếu không lấy được → coi là hệ thống
//        }
//    }
//
//    private fun loadDataForDate(date: Calendar) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val start = date.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
//            val end = date.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis
//
//            // DÙNG queryEvents ĐỂ LẤY TẤT CẢ SỰ KIỆN
//            val events = usageStatsManager.queryEvents(start, end)
//            val event = UsageEvents.Event()
//
//            val appUsageMap = mutableMapOf<String, Long>()
//
//            while (events.hasNextEvent()) {
//                events.getNextEvent(event)
//                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
//                    event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
//
//                    val packageName = event.packageName
//                    val time = event.timeStamp
//
//                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
//                        appUsageMap[packageName] = time // Bắt đầu
//                    } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
//                        val startTime = appUsageMap[packageName] ?: continue
//                        val duration = time - startTime
//                        appUsageMap[packageName] = 0L // Reset
//                        _tempUsageMap[packageName] = (_tempUsageMap[packageName] ?: 0L) + duration
//                    }
//                }
//            }
//
//            // Xử lý app vẫn đang mở (chưa paused)
//            val now = System.currentTimeMillis()
//            for ((pkg, startTime) in appUsageMap) {
//                if (startTime > 0) {
//                    val duration = now - startTime
//                    _tempUsageMap[pkg] = (_tempUsageMap[pkg] ?: 0L) + duration
//                }
//            }
//
//            // Chuyển sang UI thread để update
//            withContext(Dispatchers.Main) {
//                updateAppListFromTempMap()
//            }
//        }
//    }
//
//    private val _dateFormat = SimpleDateFormat("MMM dd, yyyy")
//    private fun updateDateSelection() {
//        _dateList.value = _dateList.value?.map {
//            it.copy(isSelected = isSameDay(it.date, selectedDate))
//        }
//    }
//
//    //update
//    /*
//    * danh sách các apps a đang thấy có các vấn đề sau
//    - Liệt kê cả các package không phải app --- ok
//    - Không thấy liệt kê các apps ko phải apps hệ thống ---
//    * */
//
//    //private var seekJob: Job? = null
//    private val _tempUsageMap = mutableMapOf<String, Long>()
//
//    // 1. seekJob: Cập nhật real-time cho ngày hiện tại
//    private fun startRealTimeTracking() {
//        seekJob?.cancel()
//        seekJob = viewModelScope.launch(Dispatchers.IO) {
//            while (isActive) {
//                loadDataForDate(selectedDate) // GỌI HÀM CŨ, NHƯNG CHỈ CHO NGÀY HIỆN TẠI
//                delay(3000)
//            }
//        }
//    }
//
//    private fun updateAppListFromTempMap() {
//        val launchableApps = getLaunchableApps() // Hàm lấy app có launcher
//        val appList = _tempUsageMap
//            .filter { (pkg, time) ->
//                time > 0 && launchableApps.contains(pkg) && !isSystemPackage(pkg)
//            }
//            .mapNotNull { (pkg, time) ->
//                try {
//                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
//                    val name = packageManager.getApplicationLabel(appInfo).toString()
//                    val icon = packageManager.getApplicationIcon(appInfo)
//                    AppUsageData(pkg, name, icon, time)
//                } catch (e: Exception) { null }
//            }
//            .sortedByDescending { it.timeUsed }
//
//        val total = appList.sumOf { it.timeUsed }
//        _uiState.value = UsageStatisticUiState(
//            date = SimpleDateFormat("MMM dd, yyyy").format(selectedDate.time),
//            totalTime = formatDuration(total),
//            compareText = calculateCompareText(selectedDate, total),
//            appList = appList.map { it.copy(percentage = if (total > 0) (it.timeUsed * 100f / total) else 0f) }
//        )
//    }
//
//    // Cache để tránh query nhiều lần
//    private var launchableAppsCache: Set<String>? = null
//
//    private fun getLaunchableApps(): Set<String> {
//        if (launchableAppsCache != null) {
//            return launchableAppsCache!!
//        }
//
//        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
//            addCategory(Intent.CATEGORY_LAUNCHER)
//        }
//
//        val launchableApps = packageManager.queryIntentActivities(mainIntent, 0)
//            .mapNotNull { it.activityInfo?.packageName }
//            .toSet()
//
//        launchableAppsCache = launchableApps
//        return launchableApps
//    }
//
//    private fun calculateCompareText(date: Calendar, todayTotal: Long): String {
//        // Lấy dữ liệu hôm qua
//        val yesterday = date.clone() as Calendar
//        yesterday.add(Calendar.DAY_OF_YEAR, -1)
//        val yesterdayStart =
//            yesterday.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
//        val yesterdayEnd =
//            yesterday.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis
//
//        val yesterdayStats =
//            usageStatsManager.queryUsageStats(INTERVAL_DAILY, yesterdayStart, yesterdayEnd)
//                ?: return "No data from yesterday"
//
//        val yesterdayTotal = yesterdayStats.sumOf { it.totalTimeInForeground }
//
//        val diff = todayTotal - yesterdayTotal
//
//        return when {
//            diff > 0 -> "+${formatDuration(diff)} more than yesterday"
//            diff < 0 -> "${formatDuration(-diff)} less than yesterday"
//            else -> "Same as yesterday"
//        }
//    }
//
//    override fun onCleared() {
//        seekJob?.cancel()
//        super.onCleared()
//    }
//
//}