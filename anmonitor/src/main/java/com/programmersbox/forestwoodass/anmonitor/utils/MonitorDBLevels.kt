package com.programmersbox.forestwoodass.anmonitor.utils

import android.content.Context
import android.util.Log
import com.programmersbox.forestwoodass.anmonitor.data.repository.SamplingSoundDataRepository
import java.util.*

class MonitorDBLevels(context: Context) {

    enum class DbDoseLength(
        val dbLevel: Float = 0.0f,
        val timeLengthInSec: Int = 0,
        val waitInterval: Long = DEFAULT_WAIT_TIME
    ) {
//        DEBUG(
//            dbLevel = 20f,
//            timeLengthInSec = 15
//        ),
        LONG_TERM(
            dbLevel = 75f,
            timeLengthInSec = 60 * 60 * 24
        ),
        VERY_LOW(
            dbLevel = 82f,
            timeLengthInSec = 60 * 60
        ),
        LOW(
            dbLevel = 86f,
            timeLengthInSec = 60 * 30,
            waitInterval = 10
        ),
        MEDIUM(
            dbLevel = 90f,
            timeLengthInSec = 60 * 15,
            waitInterval = 5
        ),
        HIGH(
            dbLevel = 95f,
            timeLengthInSec = 60 * 9,
            waitInterval = 3
        ),
        VERY_HIGH(
            dbLevel = 98f,
            timeLengthInSec = 60 * 3,
            waitInterval = 1
        )
    }

    private val repo = SamplingSoundDataRepository(context)
    private val notificationHelper = NotificationHelper(context)
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
        repo.refreshTiles()
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
        if (lastMaxDB > 0.0) {
            repo.putSampleDBValue(lastMaxDB)
            if (lastMaxDB > minAlertLevel) {
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
        } else {
            repo.refreshTiles()
        }
        return rc
    }

    private fun determineShowNotification() {
        if (showNotification) {
            if (repo.getMuteNotificationsUntil() < Calendar.getInstance().timeInMillis) {
                notificationHelper.showNotification(
                    lastMaxDB,
                    Calendar.getInstance().timeInMillis - startLevelsTimestamp
                )
                lastNotificationTime = Calendar.getInstance().timeInMillis
            } else {
                Log.d(
                    TAG,
                    "Notifications still muted for another ${(repo.getMuteNotificationsUntil() - Calendar.getInstance().timeInMillis)} ms"
                )
            }
        }
    }

    companion object {
        private const val TAG = "MonitorDBLevels"
        const val DEFAULT_WAIT_TIME = 15L
    }
}
