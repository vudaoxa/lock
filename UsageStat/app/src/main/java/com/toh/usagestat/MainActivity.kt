package com.toh.usagestat

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: AppLockViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppUsageAdapter
    private lateinit var dateText: TextView
    private lateinit var totalTimeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = AppLockViewModel()
        recyclerView = findViewById(R.id.recyclerView)
        dateText = findViewById(R.id.dateText)
        totalTimeText = findViewById(R.id.totalTimeText)

        adapter = AppUsageAdapter(emptyList()) { packageName, time ->
            viewModel.setLimit(packageName, time)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        if (!hasUsageStatsPermission()) {
            Log.d("AppLock2", "Usage Stats Permission not granted, opening Settings")
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            Log.d("AppLock2", "Usage Stats Permission granted")
        }

        viewModel.usageStats.observe(this) { stats ->
            adapter.updateData(stats)
            totalTimeText.text = "Total time: ${formatDuration(viewModel.totalTime)}"
            Log.d("AppLock2", "Observed stats size: ${stats.size}")
        }
        dateText.text = "Date: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}"

        viewModel.loadUsageStats(this)
        viewModel.startTracking(this) { appName ->
            Toast.makeText(this, "$appName has reached limit!", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        Log.d("AppLock2", "Usage Stats Permission: ${mode == AppOpsManager.MODE_ALLOWED}")
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

class AppLockViewModel : ViewModel() {
    private val _usageStats = MutableLiveData<List<AppUsageData>>(emptyList())
    val usageStats: LiveData<List<AppUsageData>> = _usageStats

    private val _limits = MutableStateFlow<Map<String, Long>>(emptyMap())
    val limits: StateFlow<Map<String, Long>> = _limits

    var totalTime: Long = 0L
        private set

    fun loadUsageStats(context: Context) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Lấy dữ liệu hôm nay
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val todayStart = todayCalendar.timeInMillis
        val todayEnd = System.currentTimeMillis()

        // Lấy dữ liệu hôm qua
        val yesterdayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val yesterdayStart = yesterdayCalendar.timeInMillis
        val yesterdayEnd = todayCalendar.timeInMillis - 1

        val todayStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, todayStart, todayEnd)
        val yesterdayStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, yesterdayStart, yesterdayEnd)

        Log.d("AppLock2", "Today Stats count: ${todayStats.size}")
        Log.d("AppLock2", "Yesterday Stats count: ${yesterdayStats.size}")

        // Tạo map để so sánh thời gian sử dụng
        val yesterdayUsageMap = yesterdayStats.associate { it.packageName to it.totalTimeInForeground }
        totalTime = todayStats.sumOf { it.totalTimeInForeground }
        val appData = todayStats.filter { it.totalTimeInForeground > 0 }.map { stat ->
            val packageManager = context.packageManager
            val appName = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(stat.packageName, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                stat.packageName
            }
            val icon = try {
                packageManager.getApplicationIcon(stat.packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            val yesterdayTime = yesterdayUsageMap[stat.packageName] ?: 0L
            val isMoreThanYesterday = stat.totalTimeInForeground > yesterdayTime
            AppUsageData(
                packageName = stat.packageName,
                appName = appName,
                timeUsed = stat.totalTimeInForeground,
                percentage = if (totalTime > 0) (stat.totalTimeInForeground * 100 / totalTime).toInt() else 0,
                icon = icon,
                isMoreThanYesterday = isMoreThanYesterday
            )
        }.sortedByDescending { it.timeUsed }
        // Lọc 3 items hàng đầu
        val top3Data = appData.take(3).map { it.copy(isTop3 = true) } + appData.drop(3)
        _usageStats.value = top3Data
        Log.d("AppLock2", "Filtered AppData count: ${top3Data.size}")
    }

    fun setLimit(packageName: String, timeLimit: Long) {
        _limits.value = _limits.value.toMutableMap().apply {
            put(packageName, timeLimit)
        }
    }

    fun startTracking(context: Context, onLimitReached: (String) -> Unit) {
        viewModelScope.launch {
            while (true) {
                loadUsageStats(context)
                limits.value.forEach { (packageName, limit) ->
                    val stat = usageStats.value?.find { it.packageName == packageName }
                    if (stat != null && stat.timeUsed >= limit) {
                        onLimitReached(stat.appName)
                    }
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }
}

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val timeUsed: Long,
    val percentage: Int,
    val icon: Any?,
    val isMoreThanYesterday: Boolean = false,
    val isTop3: Boolean = false
)

fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}