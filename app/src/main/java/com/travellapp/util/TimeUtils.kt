package com.travellapp.util

object TimeUtils {

    fun minutesToHhmm(totalMinutes: Int): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return "%02d:%02d".format(h, m)
    }

    fun hhmmToMinutes(hour: Int, minute: Int): Int = hour * 60 + minute

    fun formatDuration(minutes: Int): String = when {
        minutes < 60 -> "${minutes} min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    fun hoursRange(from: Int = 5, to: Int = 23): List<Int> = (from..to).toList()

    fun minuteSteps(step: Int = 5): List<Int> = (0..55 step step).toList()
}
