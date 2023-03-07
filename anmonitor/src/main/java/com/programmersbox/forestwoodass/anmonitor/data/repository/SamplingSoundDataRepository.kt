package com.programmersbox.forestwoodass.anmonitor.data.repository

import android.app.Service
import android.content.Context
import android.content.SharedPreferences

class SamplingSoundDataRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_FILENAME, Service.MODE_PRIVATE)
    }

    fun muteNotificationsUntil(timeInMillis: Long) {
        sharedPreferences.edit()
            .putLong(MUTE_UNTIL, timeInMillis)
            .apply()
    }

    fun getMuteNotificationsUntil(): Long {
        return sharedPreferences.getLong(
            MUTE_UNTIL, 0L
        )
    }

    companion object {
        private const val PREFERENCES_FILENAME = "sampledb_data_points"
        private const val MUTE_UNTIL = "mute_until"

    }
}
