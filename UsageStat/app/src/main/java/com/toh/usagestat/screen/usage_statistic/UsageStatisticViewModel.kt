package com.toh.usagestat.screen.usage_statistic

import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_DAILY
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toh.usagestat.screen.usage_statistic.adapter.AppUsageData
import com.toh.usagestat.screen.usage_statistic.date.DateHeaderItem
import com.toh.usagestat.screen.usage_statistic.model.UsageStatisticUiState
import com.toh.usagestat.util.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.Inject

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
        loadDataForDate(selectedDate)
    }

    fun selectDate(date: Calendar) {
        selectedDate = date
        loadDataForDate(date)
        updateDateSelection()
    }

    fun loadPreviousWeek() {
        currentStartWeek.add(Calendar.DAY_OF_YEAR, -7)
        loadWeek(currentStartWeek, addToFront = true)
    }

    fun loadNextWeek() {
        currentStartWeek.add(Calendar.DAY_OF_YEAR, 7)
        loadWeek(currentStartWeek, addToFront = false)
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
        }
    }

    private fun loadDataForDate(date: Calendar) {
        viewModelScope.launch {
            val start =
                date.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
            val end =
                date.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis

            val yesterday = date.clone() as Calendar
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStart = yesterday.apply { set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
            val yesterdayEnd = yesterday.apply { set(Calendar.HOUR_OF_DAY, 23) }.timeInMillis

            val statsToday = usageStatsManager.queryUsageStats(INTERVAL_DAILY, start, end)
            val statsYesterday =
                usageStatsManager.queryUsageStats(INTERVAL_DAILY, yesterdayStart, yesterdayEnd)
            val yesterdayMap = statsYesterday.associateBy { it.packageName }
            val totalToday = statsToday.sumOf { it.totalTimeInForeground }
            val totalYesterday = statsYesterday.sumOf { it.totalTimeInForeground }

            val appList = statsToday.mapNotNull { stat ->
                try {
                    val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)

                    val yesterdayTime = yesterdayMap[stat.packageName]?.totalTimeInForeground ?: 0L
                    val diff = stat.totalTimeInForeground - yesterdayTime

                    AppUsageData(
                        packageName = stat.packageName,
                        appName = name,
                        appIcon = icon,
                        timeUsed = stat.totalTimeInForeground,
                        moreThanYesterday = diff > 0,
                        diffWithYesterday = diff.coerceAtLeast(0L) // Chỉ hiện nếu tăng
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedByDescending { it.timeUsed }

            val total = appList.sumOf { it.timeUsed }
            val diff = totalToday - totalYesterday

            _uiState.value = UsageStatisticUiState(
                date = SimpleDateFormat("MMM dd, yyyy").format(date.time),
                totalTime = formatDuration(totalToday),
                compareText = if (diff > 0) "+${formatDuration(diff)} more than yesterday"
                else "${formatDuration(-diff)} less than yesterday",
                appList = appList.map {
                    it.copy(percentage = if (total > 0) (it.timeUsed * 100f / total) else 0f)
                }
            )
        }
    }

    private fun updateDateSelection() {
        _dateList.value = _dateList.value?.map {
            it.copy(isSelected = isSameDay(it.date, selectedDate))
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar) =
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}