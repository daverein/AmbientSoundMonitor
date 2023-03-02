package com.programmersbox.forestwoodass.anmonitor.presentation.tile

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.tiles.*
import androidx.wear.tiles.material.ChipColors
import androidx.wear.tiles.material.CompactChip
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.Typography
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.android.horologist.compose.tools.buildDeviceParameters
import com.google.android.horologist.tiles.ExperimentalHorologistTilesApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.programmersbox.forestwoodass.anmonitor.data.repository.SamplingSoundDataRepository
import com.programmersbox.forestwoodass.anmonitor.services.SamplingService
import com.programmersbox.forestwoodass.anmonitor.utils.MonitorDBLevels
import java.text.SimpleDateFormat
import java.util.*


private const val RESOURCES_VERSION = "0"
private const val GrayColor = "#80FFFFFF"
private const val IGNORE_CMD = "Ignore"
private const val REFRESH_CMD = "Refresh"

@OptIn(ExperimentalHorologistTilesApi::class)
class SamplingTile : SuspendingTileService() {
    private lateinit var repo: SamplingSoundDataRepository

    private val simpleDateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private fun Context.deviceParams() = buildDeviceParameters(resources)

    @SuppressLint("SuspiciousIndentation")
    private fun simpleLayout(
        context: Context,
        deviceParameters: DeviceParametersBuilders.DeviceParameters,
        refreshing: Boolean
        //,
        //clickable: ModifiersBuilders.Clickable
    ): LayoutElementBuilders.LayoutElement {
    val emptyClickable = ModifiersBuilders.Clickable.Builder()
        .setOnClick(ActionBuilders.LoadAction.Builder().build())
        .setId(REFRESH_CMD)
        .build()
    val ignoreClickable = ModifiersBuilders.Clickable.Builder()
        .setOnClick(ActionBuilders.LoadAction.Builder().build())
        .setId(IGNORE_CMD)
        .build()

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = repo.getSampleDBTimestamp()
    val dbString = String.format("%.1f", repo.getSampleDBValue())
    val textColor =
        if ( repo.getSampleDBValue() >= MonitorDBLevels.DbDoseLength.values().max().dbLevel ) {
            Color.Red.toArgb()
        } else if ( repo.getSampleDBValue() >= MonitorDBLevels.DbDoseLength.values().min().dbLevel ) {
                Color.Yellow.toArgb()
        } else {
            Color.White.toArgb()
        }

    val layout =  PrimaryLayout.Builder(deviceParameters)
        .setPrimaryLabelTextContent(
            Text.Builder(
                context,
                simpleDateFormat.format(calendar.time)
            )
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(ColorBuilders.argb(repo.getSamplePrimaryColor()))
                .build()
        )
        .setContent(
            Text.Builder(context, dbString)
                .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                .setColor(ColorBuilders.argb(textColor))
                .build()

        )
        .setSecondaryLabelTextContent(
            Text.Builder(context, "dB (SPL)")
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(ColorBuilders.argb(android.graphics.Color.parseColor(GrayColor)))
                .build()
        )

        if ( refreshing ) {
            layout.setPrimaryChipContent(
                CompactChip.Builder(context, "Measuring", ignoreClickable, deviceParameters)
                    .setChipColors(
                        ChipColors(
                            /*backgroundColor=*/ ColorBuilders.argb(
                                android.graphics.Color.parseColor(
                                    GrayColor
                                )
                            ),
                            /*contentColor=*/ ColorBuilders.argb(Color.White.toArgb())
                        )
                    )
                    .build()
            )
        } else {
            if ( isMyServiceRunning(SamplingService::class.java) ) {
                layout.setPrimaryChipContent(
                    CompactChip.Builder(context, " Measure ", emptyClickable, deviceParameters)
                        .setChipColors(
                            ChipColors(
                                /*backgroundColor=*/ ColorBuilders.argb(
                                    repo.getSampleSecondaryColor()
                                ),
                                /*contentColor=*/ ColorBuilders.argb(Color.White.toArgb())
                            )
                        )
                        .build()
                )
            }
        }
        return layout.build()
    }

    override fun onCreate() {
        super.onCreate()
        repo = SamplingSoundDataRepository(this)
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        var refreshing = false
        if ( requestParams.state?.lastClickableId == IGNORE_CMD ) {
            refreshing = true
        }
        if ( requestParams.state?.lastClickableId == REFRESH_CMD
                && isMyServiceRunning(SamplingService::class.java) ) {
            startForegroundService(Intent(this, SamplingService::class.java))
            refreshing = true
       }
        val singleTileTimeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(simpleLayout(baseContext, baseContext.deviceParams(), refreshing))
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(singleTileTimeline)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
             if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        fun refreshTile(context: Context)
        {
            getUpdater(context)
                .requestUpdate(SamplingTile::class.java)
        }
    }
}
