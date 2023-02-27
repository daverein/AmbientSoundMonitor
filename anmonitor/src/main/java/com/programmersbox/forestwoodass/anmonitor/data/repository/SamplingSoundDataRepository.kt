package com.programmersbox.forestwoodass.anmonitor.data.repository

import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import androidx.wear.tiles.TileService
import com.programmersbox.forestwoodass.anmonitor.presentation.tile.SamplingTile
import java.util.*

class SamplingSoundDataRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_FILENAME, Service.MODE_PRIVATE)
    }

    fun putSampleDBValue(sampleDB: Float) {

        sharedPreferences.edit()
            .putFloat(LATEST_SAMPLE, sampleDB)
            .putLong(LATEST_SAMPLE_TIME, Calendar.getInstance().timeInMillis)
            .apply()
        TileService.getUpdater(context)
            .requestUpdate(SamplingTile::class.java)
    }

    fun refreshTiles() {
        TileService.getUpdater(context)
            .requestUpdate(SamplingTile::class.java)
    }

    fun getSamplePrimaryColor(): Int {
        return sharedPreferences.getInt(
            SAMPLE_PRIMARY_COLOR,
            android.graphics.Color.parseColor("#81ACF4")
        )
    }

    fun getSampleSecondaryColor(): Int {
        return sharedPreferences.getInt(
            SAMPLE_SECONDARY_COLOR, android.graphics.Color.parseColor(
                "#e8183C75"
            )
        )
    }

    fun getSampleDBValue(): Float {
        return sharedPreferences.getFloat(LATEST_SAMPLE, 0f)
    }


    fun getSampleDBTimestamp(): Long {
        return sharedPreferences.getLong(
            LATEST_SAMPLE_TIME, 0L
        )
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
        private const val LATEST_SAMPLE = "SampleDB"
        private const val LATEST_SAMPLE_TIME = "SampleTimestampInMilli"
        private const val SAMPLE_PRIMARY_COLOR = "SamplePrimaryColor"
        private const val SAMPLE_SECONDARY_COLOR = "SampleSecondaryColor"

    }
}
