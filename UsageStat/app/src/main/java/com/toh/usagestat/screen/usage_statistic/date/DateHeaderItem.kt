package com.toh.usagestat.screen.usage_statistic.date

import java.util.Calendar

data class DateHeaderItem(
    val date: Calendar,
    val isSelected: Boolean = false
)