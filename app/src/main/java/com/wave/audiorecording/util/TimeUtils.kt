package com.wave.audiorecording.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    /**
     * Date format: 2019.09.22 11:30
     */
    private val dateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH.mm.ss", Locale.getDefault())
    fun formatTimeIntervalMinSec(length: Long): String {
        val timeUnit = TimeUnit.MILLISECONDS
        val numMinutes = timeUnit.toMinutes(length)
        val numSeconds = timeUnit.toSeconds(length)
        return String.format(Locale.getDefault(), "%02d:%02d", numMinutes, numSeconds % 60)
    }

    fun formatTimeIntervalHourMinSec2(length: Long): String {
        val timeUnit = TimeUnit.MILLISECONDS
        val numHour = timeUnit.toHours(length)
        val numMinutes = timeUnit.toMinutes(length)
        val numSeconds = timeUnit.toSeconds(length)
        return if (numHour == 0L) {
            String.format(Locale.getDefault(), "%02d:%02d", numMinutes, numSeconds % 60)
        } else {
            String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                numHour,
                numMinutes % 60,
                numSeconds % 60
            )
        }
    }

    fun formatDateForName(time: Long): String {
        return dateTimeFormat.format(Date(time))
    }

}