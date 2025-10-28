package com.toh.usagestat.screen.usage_statistic.adapter

import android.graphics.drawable.Drawable

data class AppUsageData constructor(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable,
    val timeUsed: Long,
    val percentage: Float = 0f,
    val moreThanYesterday: Boolean = false,
    val diffWithYesterday: Long = 0L  // THÊM DÒNG NÀY
)