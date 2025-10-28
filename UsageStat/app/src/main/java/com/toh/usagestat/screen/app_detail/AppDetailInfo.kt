package com.toh.usagestat.screen.app_detail

data class AppDetailInfo(
    val todayUsage: Long = 0L,
    val sessionCount: Int = 0,
    val streak: Int = 0,
    val longestSession: Long = 0L,
    val installDate: String = ""
)