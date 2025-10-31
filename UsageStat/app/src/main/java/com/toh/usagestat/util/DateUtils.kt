package com.toh.usagestat.util

import java.util.Calendar

fun formatPercentage(percentage: Float): String {
    return when {
        percentage >= 1f -> String.format("%.1f", percentage) + "%"
        percentage >= 0.1f -> String.format("%.2f", percentage) + "%"
        percentage >= 0.01f -> String.format("%.3f", percentage) + "%"
        percentage >= 0.001f -> String.format("%.4f", percentage) + "%"
        else -> String.format("%.6f", percentage) + "%"
    }
}

fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0ms"

    val hours = millis / 3_600_000L
    val remainingAfterHours = millis % 3_600_000L

    val minutes = remainingAfterHours / 60_000L
    val remainingAfterMinutes = remainingAfterHours % 60_000L

    val seconds = remainingAfterMinutes / 1_000L
    val milliseconds = remainingAfterMinutes % 1_000L

    return buildString {
        if(seconds == 0L ) append("$milliseconds" + "ms")
        if (hours > 0) append("$hours" + "h ")
        if (minutes > 0 || hours > 0) append("$minutes" + "m ")
        if (seconds > 0 || minutes > 0 || hours > 0) append("$seconds" + "s ")
    }.trim()
}

fun formatDuration__(millis: Long): String {
    if (millis <= 0) return "0s"

    val totalSeconds = millis / 1000
    val remainingMillis = millis % 1000

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0 || hours > 0) append("${minutes}m ")
        append("${seconds}s")
        if (remainingMillis > 0 && hours == 0L && minutes == 0L) {
            append(".${remainingMillis}ms")
        }
    }.trim()
}

fun formatDuration_(millis: Long): String {
    if (millis <= 0) return "0s"

    val totalSeconds = millis / 1000
    return when {
        totalSeconds < 60 -> "${totalSeconds}s"
        totalSeconds < 3600 -> {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            if (seconds == 0L) "${minutes}m" else "${minutes}m ${seconds}s"
        }

        else -> {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
        }
    }
}

fun formatDuration1(millis: Long): String {
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

