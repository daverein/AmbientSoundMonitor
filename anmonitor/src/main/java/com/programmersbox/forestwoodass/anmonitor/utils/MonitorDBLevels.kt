package com.programmersbox.forestwoodass.anmonitor.utils

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.programmersbox.forestwoodass.anmonitor.data.repository.DBLevelStore
import java.util.*

class MonitorDBLevels(val context: Context, private val warningHelper: WarningHelper?) {

    enum class DbDoseLength(
        val dbLevel: Float = 0.0f,
        val timeLengthInSec: Int = 0,
        val waitInterval: Long = DEFAULT_WAIT_TIME,
        val warningColor: Color
    ) {
//        DEBUG(
//            dbLevel = 20f,
//            timeLengthInSec = 15
//        ),
        LONG_TERM(
            dbLevel = 70f,
            timeLengthInSec = 60 * 60 * 24,
            warningColor = Color.Yellow
        ),
        VERY_LOW(
            dbLevel = 82f,
            timeLengthInSec = 60 * 60,
            warningColor = Color.Yellow
        ),
        LOW(
            dbLevel = 86f,
            timeLengthInSec = 60 * 30,
            waitInterval = 10,
            warningColor = Color.Red
        ),
        MEDIUM(
            dbLevel = 90f,
            timeLengthInSec = 60 * 15,
            waitInterval = 5,
            warningColor = Color.Red
        ),
        HIGH(
            dbLevel = 95f,
            timeLengthInSec = 60 * 9,
            waitInterval = 3,
            warningColor = Color.Red
        ),
        VERY_HIGH(
            dbLevel = 98f,
            timeLengthInSec = 60 * 3,
            waitInterval = 1,
            warningColor = Color.Red
        )
    }

    private var lastMaxDB: Float = 0.0f
    private var lastTimestamp: Long = 0L
    private var startLevelsTimestamp: Long = 0L
    private var lastNotificationTime: Long = 0L
    private var interval: Long = 0
    private val minAlertLevel = DbDoseLength.values().first().dbLevel
    private var showNotification = false

    fun getNextSampleDelay(): Long {
        return interval
    }

    fun reset() {
        interval = DEFAULT_WAIT_TIME
        startLevelsTimestamp = 0
    }

    fun recordDBLevel(maxDB: Float) {
        lastMaxDB = maxDB
        lastTimestamp = Calendar.getInstance().timeInMillis
        val waitInterval = determineWarningLevel()

        if (lastMaxDB >= minAlertLevel && !showNotification) {
            interval = waitInterval
        } else {
            interval = DEFAULT_WAIT_TIME
            if ( lastMaxDB < minAlertLevel ) {
                startLevelsTimestamp = 0
            }
        }
    }

    private fun determineWarningLevel(): Long {
        var rc = DEFAULT_WAIT_TIME
        if (lastMaxDB > 0.0 && lastMaxDB > minAlertLevel) {
            if (startLevelsTimestamp == 0L) {
                startLevelsTimestamp = Calendar.getInstance().timeInMillis
            }
            showNotification = false
            DbDoseLength.values().forEach {
                if (it.dbLevel <= lastMaxDB ) {
                    rc = it.waitInterval
                    if ((it.timeLengthInSec * 1000 + startLevelsTimestamp) <= Calendar.getInstance().timeInMillis) {
                         showNotification = true
                    }
                }
            }
            determineShowNotification()
        }
        return rc
    }

    fun minutesInRange(minValue: Float, maxValue: Float, samples: ArrayList<DBLevelStore.SampleValue>): Int {
        var lastTimestamp = 0L
        var totalSeconds = 0
        samples.filter {
            it.sampleValue >= minValue && it.sampleValue < maxValue
        }.sortedWith(compareBy { it.timestamp })
            // Need to sort asc by timestamp here
        .forEach {
            if ( lastTimestamp == 0L ) {
                Log.d("MinutesInRange", "Starting ${DateFormat.format("MMM dd hh:mm:ss", it.timestamp)}")
                lastTimestamp = it.timestamp
            } else {
                Log.d("MinutesInRange", "next ${DateFormat.format("MMM dd hh:mm:ss", it.timestamp)}")
                Log.d("MinutesInRange", "checking = ${it.timestamp - lastTimestamp}")
                if ( it.timestamp < lastTimestamp + (1000*60*20) ) {
                    totalSeconds += ((it.timestamp - lastTimestamp)/1000).toInt()
                    Log.d("MinutesInRange", "Adding totalsec = ${it.timestamp - lastTimestamp}")
                    Log.d("MinutesInRange", "so far totalsec = ${totalSeconds/60} minutes")
                } else {
                    totalSeconds += (60*15)
                    lastTimestamp = 0L
                }
                lastTimestamp = it.timestamp
            }
        }
        if ( lastTimestamp != 0L )
            totalSeconds += (60*15)
        Log.d("MinutesInRange", "totalsec = $totalSeconds")
        return totalSeconds
    }

    private fun determineShowNotification() {
        if (showNotification) {
            warningHelper?.showNotification(
                lastMaxDB,
                Calendar.getInstance().timeInMillis,
                Calendar.getInstance().timeInMillis - startLevelsTimestamp
            )
            lastNotificationTime = Calendar.getInstance().timeInMillis
        }
    }

    companion object {
        const val DEFAULT_WAIT_TIME = 15L
    }
}
