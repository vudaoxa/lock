package com.toh.usagestat.screen.usage_statistic

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_DAILY
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toh.usagestat.screen.usage_statistic.adapter.AppUsageData
import com.toh.usagestat.screen.usage_statistic.date.DateHeaderItem
import com.toh.usagestat.screen.usage_statistic.model.UsageStatisticUiState
import com.toh.usagestat.util.formatDuration
import com.toh.usagestat.util.getOffsetDate
import com.toh.usagestat.util.isAfter
import com.toh.usagestat.util.isBefore
import com.toh.usagestat.util.isSameDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

//queryEvents by gpt
@HiltViewModel
class UsageStatisticViewModel @Inject constructor(
    private val usageStatsManager: UsageStatsManager,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _uiState = MutableLiveData<UsageStatisticUiState>()
    val uiState: LiveData<UsageStatisticUiState> = _uiState

    private val _dateList = MutableLiveData<List<DateHeaderItem>>()
    val dateList: LiveData<List<DateHeaderItem>> = _dateList

    private var currentStartWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
    private var selectedDate = Calendar.getInstance()

    init {
        loadInitialWeek()
        //loadInitialDate()
    }

    //fun smoothScrollToDate(): Int {
    //    return dateList.value.map { it.date }.indexOf(selectedDate)
    //}

    fun loadInitialDate() {
        _uiState.value = UsageStatisticUiState(
            //date = _dateFormat.format(Calendar.getInstance().time),
            date = _dateFormat.format(selectedDate.time),
        )
    }

    //gpt
    fun loadInitialData() {
        loadDataForDate(selectedDate)
        //getUsedAppsToday()
    }


    //fun loadInitialData1() {
    //    loadDataForDate(selectedDate)
    //    startRealTimeTracking() // BẮT ĐẦU THEO DÕI
    //}

    fun selectDate(date: Calendar) {
        selectedDate = date
        updateDateSelection()
        loadDataForDate(date)
    }

    fun loadPreviousWeek() {
        currentStartWeek.add(Calendar.DAY_OF_YEAR, -7)
        loadWeek(currentStartWeek, addToFront = true)
    }

    fun loadNextWeek() {
        currentStartWeek.add(Calendar.DAY_OF_YEAR, 7)
        loadWeek(currentStartWeek, addToFront = false)
    }

    fun selectDateOffset(offset: Int) {
        if ((offset < 0 && selectedDate.isAfter(_dateList.value.first().date))
            || offset > 0 && selectedDate.isBefore(_dateList.value.last().date)
        ) {
            selectDate(selectedDate.getOffsetDate(offset))
        }
    }

    fun selectLastDate(): Boolean {
        selectDate(_dateList.value.last().date)
        return true
    }

    fun selectFirstDate(): Boolean {
        selectDate(_dateList.value.first().date)
        return true
    }

    private fun loadInitialWeek() {
        currentStartWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        loadWeek(currentStartWeek, addToFront = false)
    }

    private fun loadWeek(start: Calendar, addToFront: Boolean) {
        viewModelScope.launch {
            val newWeek = mutableListOf<DateHeaderItem>()
            val tempStart = start.clone() as Calendar
            for (i in 0..6) {
                val date = tempStart.clone() as Calendar
                date.add(Calendar.DAY_OF_YEAR, i)
                newWeek.add(DateHeaderItem(date, isSameDay(date, selectedDate)))
            }

            val current = _dateList.value?.toMutableList() ?: mutableListOf()
            if (addToFront) {
                current.addAll(0, newWeek)
            } else {
                current.addAll(newWeek)
            }

            // Giữ 35 ngày (5 tuần)
            val trimmed = if (current.size > 35) current.takeLast(35) else current
            _dateList.value = trimmed
            //_dateList.value = current
        }
    }

    private var seekJob: Job? = null

    // Hàm lọc package hệ thống
    private fun isSystemPackage(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            true // Nếu không lấy được → coi là hệ thống
        }
    }
    private fun loadDataForDate(date: Calendar) {
        viewModelScope.launch(Dispatchers.IO) {
            val start = date.clone() as Calendar
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)
            val startTime = start.timeInMillis

            val end = date.clone() as Calendar
            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)
            val endTime = end.timeInMillis

            // --- 1️⃣ Lấy toàn bộ app user cài ---
            val userApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { app ->
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    !(isSystem || isUpdatedSystem)
                }
                .map { it.packageName }
                .toSet()

            // --- 2️⃣ Lấy các app có thống kê usage trong ngày đó ---
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime, endTime
            ) ?: emptyList()


            val usedApps = usageStats.map { it.packageName }.toSet()

            // --- 3️⃣ Giao hai tập để chỉ giữ app user đã từng chạy ---
            val userAppsUsed = userApps.intersect(usedApps)


            // --- 4️⃣ Gộp các usage stats theo package trước khi map ---
            val mergedStats = usageStats
                .filter { it.packageName in userAppsUsed && it.totalTimeInForeground > 0}
                .groupBy { it.packageName }
                .mapValues { entry ->
                    entry.value.sumOf { it.totalTimeInForeground }
                }

            // --- 4️⃣ Map ra AppUsageData ---
            //val appList = mergedStats
            //    //.filter { it.packageName in userAppsUsed && it.totalTimeInForeground > 0}
            //    //.distinctBy { it.packageName }
            //    .mapNotNull { stat ->
            //        try {
            //            val info = packageManager.getApplicationInfo(stat.packageName, 0)
            //            val label = packageManager.getApplicationLabel(info).toString()
            //            val icon = packageManager.getApplicationIcon(info)
            //            AppUsageData(
            //                packageName = stat.packageName,
            //                appName = label,
            //                appIcon = icon,
            //                timeUsed = stat.totalTimeInForeground
            //            )
            //        } catch (e: Exception) {
            //            null
            //        }
            //    }
            //    .sortedByDescending { it.timeUsed }

            // --- Map ra AppUsageData ---
            val appList = mergedStats
                //.filterValues { it > 0L } // bỏ app có thời gian 0
                .mapNotNull { (pkg, totalTime) ->
                try {
                    val info = packageManager.getApplicationInfo(pkg, 0)
                    val label = packageManager.getApplicationLabel(info).toString()
                    val icon = packageManager.getApplicationIcon(info)
                    AppUsageData(
                        packageName = pkg,
                        appName = label,
                        appIcon = icon,
                        timeUsed = totalTime
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.timeUsed }

            val total = appList.sumOf { it.timeUsed }

            // --- 5️⃣ Cập nhật UI ---
            withContext(Dispatchers.Main) {
                _uiState.value = UsageStatisticUiState(
                    date = _dateFormat.format(date.time),
                    totalTime = formatDuration(total),
                    compareText = calculateCompareText(date, total),
                    appList = appList.map {
                        it.copy(percentage = if (total > 0) (it.timeUsed * 100f / total) else 0f)
                    }
                )
            }
        }
    }

    //private fun loadDataForDate_(date: Calendar) {
    //    viewModelScope.launch(Dispatchers.IO) {
    //        val start = date.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
    //        val end = date.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis
    //
    //        // DÙNG queryEvents ĐỂ LẤY TẤT CẢ SỰ KIỆN
    //        val events = usageStatsManager.queryEvents(start, end)
    //        val event = UsageEvents.Event()
    //
    //        val appUsageMap = mutableMapOf<String, Long>()
    //
    //        while (events.hasNextEvent()) {
    //            events.getNextEvent(event)
    //            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
    //                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
    //
    //                val packageName = event.packageName
    //                val time = event.timeStamp
    //                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
    //                    appUsageMap[packageName] = time // Bắt đầu
    //                }
    //                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
    //                    appUsageMap[packageName] = time // Bắt đầu
    //                } else if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
    //                    val startTime = appUsageMap[packageName] ?: continue
    //                    val duration = time - startTime
    //                    appUsageMap[packageName] = 0L // Reset
    //                    _tempUsageMap[packageName] = (_tempUsageMap[packageName] ?: 0L) + duration
    //                }
    //            }
    //        }
    //
    //        // Xử lý app vẫn đang mở (chưa paused)
    //        val now = System.currentTimeMillis()
    //        for ((pkg, startTime) in appUsageMap) {
    //            if (startTime > 0) {
    //                val duration = now - startTime
    //                _tempUsageMap[pkg] = (_tempUsageMap[pkg] ?: 0L) + duration
    //            }
    //        }
    //
    //        // Chuyển sang UI thread để update
    //        withContext(Dispatchers.Main) {
    //            updateAppListFromTempMap()
    //        }
    //    }
    //}

    private val _dateFormat = SimpleDateFormat("MMM dd, yyyy")
    private fun updateDateSelection() {
        _dateList.value = _dateList.value?.map {
            it.copy(isSelected = isSameDay(it.date, selectedDate))
        }
    }

    //update
    /*
    * danh sách các apps a đang thấy có các vấn đề sau
    - Liệt kê cả các package không phải app --- ok
    - Không thấy liệt kê các apps ko phải apps hệ thống ---
    * */

    //private var seekJob: Job? = null
    private val _tempUsageMap = mutableMapOf<String, Long>()

    // 1. seekJob: Cập nhật real-time cho ngày hiện tại
    //private fun startRealTimeTracking() {
    //    seekJob?.cancel()
    //    seekJob = viewModelScope.launch(Dispatchers.IO) {
    //        while (isActive) {
    //            loadDataForDate(selectedDate) // GỌI HÀM CŨ, NHƯNG CHỈ CHO NGÀY HIỆN TẠI
    //            delay(3000)
    //        }
    //    }
    //}

    //private fun updateAppListFromTempMap() {
    //    val launchableApps = getLaunchableApps() // Hàm lấy app có launcher
    //    val appList = _tempUsageMap
    //        .filter { (pkg, time) ->
    //            time > 0 && launchableApps.contains(pkg) && !isSystemPackage(pkg)
    //        }
    //        .mapNotNull { (pkg, time) ->
    //            try {
    //                val appInfo = packageManager.getApplicationInfo(pkg, 0)
    //                val name = packageManager.getApplicationLabel(appInfo).toString()
    //                val icon = packageManager.getApplicationIcon(appInfo)
    //                AppUsageData(pkg, name, icon, time)
    //            } catch (e: Exception) { null }
    //        }
    //        .sortedByDescending { it.timeUsed }
    //
    //    val total = appList.sumOf { it.timeUsed }
    //    _uiState.value = UsageStatisticUiState(
    //        date = SimpleDateFormat("MMM dd, yyyy").format(selectedDate.time),
    //        totalTime = formatDuration(total),
    //        compareText = calculateCompareText(selectedDate, total),
    //        appList = appList.map { it.copy(percentage = if (total > 0) (it.timeUsed * 100f / total) else 0f) }
    //    )
    //}

    // Cache để tránh query nhiều lần
    //private var launchableAppsCache: Set<String>? = null

    //private fun getLaunchableApps(): Set<String> {
    //    if (launchableAppsCache != null) {
    //        return launchableAppsCache!!
    //    }
    //
    //    val mainIntent = Intent(Intent.ACTION_MAIN).apply {
    //        addCategory(Intent.CATEGORY_LAUNCHER)
    //    }
    //
    //    val launchableApps = packageManager.queryIntentActivities(mainIntent, 0)
    //        .mapNotNull { it.activityInfo?.packageName }
    //        .toSet()
    //
    //    launchableAppsCache = launchableApps
    //    return launchableApps
    //}

    private fun calculateCompareText(date: Calendar, todayTotal: Long): String {
        // Lấy dữ liệu hôm qua
        val yesterday = date.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart =
            yesterday.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
        val yesterdayEnd =
            yesterday.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis

        val yesterdayStats =
            usageStatsManager.queryUsageStats(INTERVAL_DAILY, yesterdayStart, yesterdayEnd)
                ?: return "No data from yesterday"

        val yesterdayTotal = yesterdayStats.sumOf { it.totalTimeInForeground }

        val diff = todayTotal - yesterdayTotal

        return when {
            diff > 0 -> "+${formatDuration(diff)} more than yesterday"
            diff < 0 -> "${formatDuration(-diff)} less than yesterday"
            else -> "Same as yesterday"
        }
    }

    override fun onCleared() {
        seekJob?.cancel()
        super.onCleared()
    }

    //gpt

    private fun getUsedAppsToday() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        // --- 1. Lấy toàn bộ app user cài ---
        val userApps = packageManager.getInstalledApplications(0)
            .filter { app ->
                // bỏ system app
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .map { it.packageName }
            .toSet()

        // --- 2. Lấy các app từng chạy ---
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime, endTime
        )

        val usedApps = usageStats.map { it.packageName }.toSet()

        // --- 3. Giao 2 tập ---
        val userAppsUsed = userApps.intersect(usedApps)

        // Debug xem kết quả
        Log.d("USER_APPS_USED", "Số app user đã chạy: ${userAppsUsed.size}")
        userAppsUsed.forEach { Log.d("USER_APPS_USED", it) }
    }

    @SuppressLint("WrongConstant")
    private fun getUsedAppsToday_(): Pair<List<AppUsageData>, String?> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        //val events = usageStatsManager.queryEvents(startTime, endTime)
        //val event = UsageEvents.Event()
        //
        //val usageMap = mutableMapOf<String, Long>()
        //val lastForegroundTime = mutableMapOf<String, Long>()

        var currentForegroundApp: String? = null
        // Lấy danh sách usage stats như cũ
        val statsToday = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime, endTime
        ) ?: emptyList()

        // --- Bổ sung query app khả dụng ---
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

                val yesterday = cal.clone() as Calendar
                yesterday.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStart =
                    yesterday.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
                val yesterdayEnd =
                    yesterday.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis

                //val yesterdayStats =
                //    usageStatsManager.queryUsageStats(INTERVAL_DAILY, yesterdayStart, yesterdayEnd)
                        //?: return "No data from yesterday"

                //val yesterdayTotal = yesterdayStats.sumOf { it.totalTimeInForeground }

        val statsYesterday =
            usageStatsManager.queryUsageStats(INTERVAL_DAILY, yesterdayStart, yesterdayEnd)
        val yesterdayMap = statsYesterday.associateBy { it.packageName }

        // Tất cả activity có thể mở được (launcher hoặc default)
        val resolvedActivities = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        val launchablePkgs = resolvedActivities.map { it.activityInfo.packageName }.toSet()

        // Các app user cài (loại bỏ system app)
        //val userInstalledPkgs = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        //    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        //    .map { it.packageName }

        val userInstalledPkgs = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                !(isSystem || isUpdatedSystem) // bỏ app hệ thống và system được update
            }
            .map { it.packageName }

        // Hợp hai tập này để lấy danh sách app "có ý nghĩa"
        val combinedPkgs = (launchablePkgs + userInstalledPkgs).toSet()

        // --- Lọc ra app nào user có dùng ---
        val appList = statsToday
            .filter { combinedPkgs.contains(it.packageName) } // lọc các app "thật"
            .mapNotNull { stat ->
                try {
                    val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)

                    val yesterdayTime =
                        yesterdayMap[stat.packageName]?.totalTimeInForeground ?: 0L
                    val diff = stat.totalTimeInForeground - yesterdayTime

                    AppUsageData(
                        packageName = stat.packageName,
                        appName = name,
                        appIcon = icon,
                        timeUsed = stat.totalTimeInForeground,
                        moreThanYesterday = diff > 0,
                        diffWithYesterday = diff.coerceAtLeast(0L)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedByDescending { it.timeUsed }
        return Pair(appList, currentForegroundApp).also { (appList, currentForegroundApp) -> print("size = "+appList.size) }

    }

    private fun getUsedAppsToday1(): Pair<List<AppUsageData>, String?> {
        val pm = packageManager

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        val usageMap = mutableMapOf<String, Long>()
        val lastForegroundTime = mutableMapOf<String, Long>()

        var currentForegroundApp: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                // --- Các event kiểu mới Android 14 ---
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                    // --- Các event cũ (fallback cho Android <= 13, MIUI, One UI) ---
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {

                    val pkg = event.packageName

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED,
                        UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                            lastForegroundTime[pkg] = event.timeStamp
                            currentForegroundApp = pkg // app mới lên foreground
                        }

                        UsageEvents.Event.ACTIVITY_STOPPED,
                        UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                            val start = lastForegroundTime[pkg]
                            if (start != null) {
                                val diff = event.timeStamp - start
                                usageMap[pkg] = (usageMap[pkg] ?: 0L) + diff
                                lastForegroundTime.remove(pkg)
                            }
                        }
                    }
                }
            }
        }

        // --- Tạo danh sách kết quả ---
        val appList = usageMap.mapNotNull { (pkg, timeUsed) ->
            try {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                //if (launchIntent != null && timeUsed > 0) {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    AppUsageData(
                        packageName = pkg,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        appIcon = pm.getApplicationIcon(pkg),
                        timeUsed = timeUsed
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.timeUsed }
        return Pair(appList, currentForegroundApp).also { (appList, currentForegroundApp) -> print("size = "+appList.size) }
    }

}