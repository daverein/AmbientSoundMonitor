package com.programmersbox.forestwoodass.anmonitor.utils

import android.app.ActivityManager
import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material.*
import java.util.*

const val COLORS_GREEN_START = 0.0f
const val COLORS_YELLOW_START = 0.70f
const val COLORS_RED_START = 0.85f

@Suppress("DEPRECATION")
fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        Log.d("isMyServiceRunning", "Service: ${service.service.className}")
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

@Composable
fun FormatTimeText(weekView: Boolean, timestamp: Long = 0L) {
    val leadingTextStyle =
        TimeTextDefaults.timeTextStyle(color = MaterialTheme.colors.primary, fontSize = 11.sp)
    val leadingTextStyle2 =
        TimeTextDefaults.timeTextStyle(color = MaterialTheme.colors.secondary, fontSize = 11.sp)
    val timeTextFirstPart: String
    val timeTextSecondPart: String
    if (weekView) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val startTime = cal.timeInMillis - ((dow - 1) * (1000 * 60 * 60 * 24))
        val endTime = cal.timeInMillis + ((7 - dow) * (1000 * 60 * 60 * 24))
        timeTextFirstPart = "${DateFormat.format("MMM dd", startTime)} - "
        timeTextSecondPart = " ${DateFormat.format("MMM dd", endTime)}"
    } else {
        val cal: Calendar
        if ( timestamp == 0L) {
            cal = Calendar.getInstance()
        } else {
            cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            if ( Calendar.getInstance().get(Calendar.MONTH) != cal.get(Calendar.MONTH) ||
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH) != cal.get(Calendar.DAY_OF_MONTH)) {
                // Don't show the date here
                TimeText(
                    timeSource = TimeTextDefaults.timeSource(
                        DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm")
                    )
                )
                return
            }
        }
        val month: String = DateFormat.format("MMM", cal.timeInMillis).toString()
        val day: String = DateFormat.format(" dd", cal.timeInMillis).toString()
        timeTextFirstPart = month
        timeTextSecondPart = day
    }

    TimeText(
        timeSource = TimeTextDefaults.timeSource(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm")
        ),
        timeTextStyle = leadingTextStyle,
        startLinearContent = {
            Text(
                text = timeTextFirstPart,
                style = (leadingTextStyle2)
            )
            Text(
                text = timeTextSecondPart,
                style = (leadingTextStyle2)
            )
        },
        startCurvedContent = {
            curvedText(
                text = timeTextFirstPart,
                style = CurvedTextStyle(leadingTextStyle2)
            )
            curvedText(
                text = timeTextSecondPart,
                style = CurvedTextStyle(leadingTextStyle2)
            )
        },
    )
}