package com.toh.usagestat.screen.app_detail

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toh.usagestat.screen.app_detail.model.AppDetailInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val usageStatsManager: UsageStatsManager,
    private val packageManager: PackageManager
) : ViewModel() {
    private val packageName: String = savedStateHandle["packageName"]!!
    private val _appInfo = MutableLiveData<AppDetailInfo>()
    val appInfo: LiveData<AppDetailInfo> = _appInfo

    init {
        loadAppDetail()
    }

    private fun loadAppDetail() {
        viewModelScope.launch {
            _appInfo.value = getAppDetailInfo(packageName)
        }
    }

    fun getAppDetailInfo(packageName: String): AppDetailInfo {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val sevenDaysAgo = calendar.timeInMillis
        val now = System.currentTimeMillis()

        var todayUsage = 0L
        var sessionCount = 0
        var longestSession = 0L
        var currentSessionStart = 0L
        val dailyUsage = mutableMapOf<Long, Long>()

        val events = usageStatsManager.queryEvents(sevenDaysAgo, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    currentSessionStart = event.timeStamp
                    sessionCount++
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (currentSessionStart > 0) {
                        val duration = event.timeStamp - currentSessionStart
                        longestSession = maxOf(longestSession, duration)
                        val day = event.timeStamp / (24 * 60 * 60 * 1000)
                        dailyUsage[day] = (dailyUsage[day] ?: 0) + duration
                        if (event.timeStamp >= getTodayStart()) todayUsage += duration
                        currentSessionStart = 0
                    }
                }
            }
        }

        val streak = calculateStreak(dailyUsage)
        val installDate = SimpleDateFormat("MM/dd/yy").format(
            Date(
                packageManager.getPackageInfo(
                    packageName,
                    0
                ).firstInstallTime
            )
        )

        return AppDetailInfo(todayUsage, sessionCount, streak, longestSession, installDate)
    }

    private fun calculateStreak(dailyUsage: Map<Long, Long>): Int {
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
        var streak = 0
        var day = today
        while ((dailyUsage[day] ?: 0) > 0) {
            streak++
            day--
        }
        return streak
    }

    private fun getTodayStart(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}