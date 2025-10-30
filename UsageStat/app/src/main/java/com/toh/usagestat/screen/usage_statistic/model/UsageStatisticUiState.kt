package com.toh.usagestat.screen.usage_statistic.model

import com.toh.usagestat.screen.usage_statistic.adapter.AppUsageData

data class UsageStatisticUiState constructor(
    val date: String = "",
    val totalTime: String = "0s",
    val compareText: String = "",
    val appList: List<AppUsageData> = emptyList()
)