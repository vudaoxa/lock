package com.toh.usagestat.util

import java.util.Calendar

fun formatDuration(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis % 3600000) / 60000
    val seconds = (millis % 60000) / 1000
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar) =
    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

fun Calendar.getOffsetDate(days: Int): Calendar {
    return (this.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, days)
    }
}

fun Calendar.isBefore(cal2: Calendar) =
    get(Calendar.YEAR) < cal2.get(Calendar.YEAR) ||
            (get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    get(Calendar.DAY_OF_YEAR) < cal2.get(Calendar.DAY_OF_YEAR))

fun Calendar.isAfter(cal2: Calendar) =
    get(Calendar.YEAR) > cal2.get(Calendar.YEAR) ||
            (get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    get(Calendar.DAY_OF_YEAR) > cal2.get(Calendar.DAY_OF_YEAR))

