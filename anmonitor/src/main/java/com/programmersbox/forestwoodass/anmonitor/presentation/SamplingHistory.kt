package com.programmersbox.forestwoodass.anmonitor.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import com.google.accompanist.pager.*
import com.programmersbox.forestwoodass.anmonitor.R
import com.programmersbox.forestwoodass.anmonitor.data.repository.DBLevelStore
import com.programmersbox.forestwoodass.anmonitor.utils.COLORS_RED_START
import com.programmersbox.forestwoodass.anmonitor.utils.COLORS_YELLOW_START
import com.programmersbox.forestwoodass.anmonitor.utils.FormatTimeText
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val TAG = "SamplingHistory"
private const val GraphColor = "#88888888"

@Composable
fun SamplingHistory(weekView: Boolean, dow: Int = -1) {
    val context = LocalContext.current
    val dbHelper = DBLevelStore(LocalContext.current)
    val samples = dbHelper.getAllSamples(weekView, dow)
    Log.d(TAG, "Got ${samples.size} items")

    if ( dow != -1 && samples.size == 0 ) {
        ShowNoSamplesDialog(context)
        return
    }
    var minValue = 120f
    var maxValue = 0f
    samples.forEach {
        minValue = min(minValue, it.sampleValue)
        maxValue = max(maxValue, it.sampleValue)
    }
    if ( samples.size > 0 ) {
        FormatTimeText(weekView, samples[0].timestamp, dowIn = dow)
    } else {
        FormatTimeText(weekView, dowIn = dow)
    }
    Column(
        modifier = Modifier
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.padding(12.dp))
        DrawChartTitle(weekView, dow, samples)
        if ( samples.size > 0 ) {
            DrawSampleRange(minValue, maxValue)
            Spacer(Modifier.padding(1.dp))
            ChartLevels(weekView, dow, samples)

        }
        Spacer(Modifier.padding(2.dp))
        Text(buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.secondary
                )
            ) {
                append("No warnings")
            }
        })
    }
}

@Composable
private fun DrawChartTitle(
    weekView: Boolean,
    dow: Int,
    samples: ArrayList<DBLevelStore.SampleValue>
) {
    Text(buildAnnotatedString {
        withStyle(style = SpanStyle(fontSize = 12.sp, color = Color.LightGray)) {
            if (weekView) {
                append("This Week")
            } else {
                if (dow == -1) {
                    append("Today")
                } else {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = samples[0].timestamp
                    append("${DateFormat.format("EEE MMM dd", cal)}")
                }
            }
        }
    })
}

@Composable
private fun ShowNoSamplesDialog(context: Context) {
    Alert(
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 52.dp),
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.hearing_damage),
                contentDescription = "Hearing Damage",
                modifier = Modifier
                    .size(16.dp)
                    .wrapContentSize(align = Alignment.Center),
            )
        },
        title = { Text(text = "Ambient Monitor", textAlign = TextAlign.Center) },
        message = {
            Text(
                text = "No data exists for that period",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2
            )
        },
    ) {
        item {
            Chip(
                label = { Text("Close") },
                onClick = {
                    val activity = (context as? Activity)
                    activity?.finish()
                },
                colors = ChipDefaults.primaryChipColors(),
            )
        }
    }
}

@Composable
private fun DrawSampleRange(minValue: Float, maxValue: Float) {
    Text(buildAnnotatedString {
        withStyle(style = SpanStyle(fontSize = 21.sp, color = MaterialTheme.colors.primary)) {
            append(
                String.format(
                    "%.1f",
                    minValue
                )
            )
        }
        withStyle(style = SpanStyle(fontSize = 8.sp, color = MaterialTheme.colors.primary)) {
            append("dB")
        }
        withStyle(style = SpanStyle(fontSize = 21.sp, color = MaterialTheme.colors.primary)) {
            append(
                String.format(
                    " - %.1f",
                    maxValue
                )
            )
        }
        withStyle(style = SpanStyle(fontSize = 8.sp, color = MaterialTheme.colors.primary)) {
            append("dB")
        }
    })
}


@OptIn(ExperimentalTextApi::class)
@Composable
fun ChartLevels(weekView: Boolean, dow: Int, samples: ArrayList<DBLevelStore.SampleValue>) {
    val hoursView = when (weekView) {
        true -> 7
        false -> 24
    }
    val context = LocalContext.current
    val textMeasure = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .padding(PaddingValues(15.dp, 0.dp))
            .background(MaterialTheme.colors.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        // When the user taps on the Canvas, you can
                        // check if the tap offset is in one of the
                        // tracked Rects.
                        if ( weekView ) {
                            val mDow = floor((tapOffset.x /size.width.toFloat())*7).toInt()
                            Log.d(TAG, "Tapped offset = ${tapOffset.x}  days back = $mDow")
                            context.startActivity(Intent(context, SampleDayDialog::class.java).putExtra("DOW", mDow))
                        }
                    }
                )
            }
    ) {
        val chartHeight = size.height - 25
        val chartWidth = size.width
        val startingOffset = ((chartWidth / hoursView) / 2f)
        drawBaseChart(
            chartWidth,
            chartHeight,
            hoursView,
            startingOffset,
            weekView,
            textMeasure
        )

        if ( dow ==  -1 ) {
            drawTodayLine(weekView, startingOffset, chartWidth, chartHeight)
        }

        Log.d(DBMonitor.TAG, "Got ${samples.size} items")
        if ( !weekView ) {
            drawDailyChart(samples, startingOffset, chartWidth, chartHeight)
        } else {
            drawWeeklyChart(samples, startingOffset/3, chartWidth, chartHeight)
        }
    }
}

private fun DrawScope.drawWeeklyChart(
    samples: ArrayList<DBLevelStore.SampleValue>,
    startingOffset: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val cal = Calendar.getInstance()
    var minValue = 120f
    var maxValue = 0f
    var dayMin = 99f
    var dayMax = 0f
    var dayCurrent = -1
    var dayPart = -1.0
    samples.forEach {
        cal.timeInMillis = it.timestamp
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
        val hod = cal.get(Calendar.HOUR_OF_DAY)
        val dp = floor(hod/8.0)

        if (dow != dayCurrent || dayPart != dp) {
            if (dayCurrent != -1 && dayPart != -1.0) {
                val x: Float = ((dayCurrent / 7f)   + dayPart *(1/21f)).toFloat()
                Log.d(TAG, "X = $x  $dayCurrent  and $dayPart with $dayMin / $dayMax")
                drawLine(
                    Color(android.graphics.Color.parseColor(GraphColor)),
                    Offset(
                        startingOffset + (x) * chartWidth,
                        chartHeight - (dayMin / 90f) * chartHeight
                    ),
                    Offset(
                        startingOffset + (x) * chartWidth,
                        chartHeight - (dayMax / 90f) * chartHeight
                    ),
                    cap = StrokeCap.Round,
                    strokeWidth = 8f
                )
            }
            dayCurrent = dow
            dayPart = dp
            dayMin = 99f
            dayMax = 0f
        }
        val x: Float = ((dow / 7f)   + floor(hod/8.0) *(1/21f)).toFloat()
        val sampleValue = it.sampleValue - 10f

        val ballColor = getBallColor(it)
        drawCircle(
            ballColor, 4f,
            Offset(
                startingOffset + +(chartWidth * (x)),
                chartHeight - (sampleValue / 90f) * chartHeight
            )
        )
        minValue = min(minValue, it.sampleValue)
        maxValue = max(maxValue, it.sampleValue)
        dayMin = min(dayMin, sampleValue)
        dayMax = max(dayMax, sampleValue)
    }

    val x = (dayCurrent / 7f)   + (dayPart *(1/21f))
    Log.d(TAG, "X = $x  $dayCurrent  and $dayPart  with $dayMin / $dayMax")
    drawLine(
        Color(android.graphics.Color.parseColor(GraphColor)),
        Offset(
            (startingOffset + (x) * chartWidth).toFloat(),
            chartHeight - (dayMin / 90f) * chartHeight
        ),
        Offset(
            (startingOffset + (x) * chartWidth).toFloat(),
            chartHeight - (dayMax / 90f) * chartHeight
        ),
        cap = StrokeCap.Round,
        strokeWidth = 6f
    )
}

private fun getBallColor(it: DBLevelStore.SampleValue): Color {
    return if (it.sampleValue < COLORS_YELLOW_START * 100f) {
        Color.Green
    } else if (it.sampleValue < COLORS_RED_START * 100f) {
        Color.Yellow
    } else {
        Color.Red
    }
}


private fun DrawScope.drawDailyChart(
    samples: ArrayList<DBLevelStore.SampleValue>,
    startingOffset: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val cal = Calendar.getInstance()
    var minValue = 120f
    var maxValue = 0f
    var hourMin = 99f
    var hourMax = 0f
    var hourCurrent = -1
    samples.forEach {
        cal.timeInMillis = it.timestamp
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        if (hour != hourCurrent) {
            if (hourCurrent != -1) {
                drawLine(
                    Color(android.graphics.Color.parseColor(GraphColor)),
                    Offset(
                        startingOffset + (hourCurrent / 24f) * chartWidth,
                        chartHeight - (hourMin / 90f) * chartHeight
                    ),
                    Offset(
                        startingOffset + (hourCurrent / 24f) * chartWidth,
                        chartHeight - (hourMax / 90f) * chartHeight
                    ),
                    cap = StrokeCap.Round,
                    strokeWidth = 8f
                )
            }
            hourCurrent = hour
            hourMin = 99f
            hourMax = 0f
        }
        val x = (hour / 24f)
        val sampleValue = it.sampleValue - 10f

        val ballColor = getBallColor(it)

        drawCircle(
            ballColor, 4f,
            Offset(
                startingOffset + +(chartWidth * (x)),
                chartHeight - (sampleValue / 90f) * chartHeight
            )
        )
        minValue = min(minValue, it.sampleValue)
        maxValue = max(maxValue, it.sampleValue)
        hourMin = min(hourMin, sampleValue)
        hourMax = max(hourMax, sampleValue)
    }
    drawLine(
        Color(android.graphics.Color.parseColor(GraphColor)),
        Offset(
            startingOffset + (hourCurrent / 24f) * chartWidth,
            chartHeight - (hourMin / 90f) * chartHeight
        ),
        Offset(
            startingOffset + (hourCurrent / 24f) * chartWidth,
            chartHeight - (hourMax / 90f) * chartHeight
        ),
        cap = StrokeCap.Round,
        strokeWidth = 6f
    )
}


@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawBaseChart(
    chartWidth: Float,
    chartHeight: Float,
    hoursView: Int,
    startingOffset: Float,
    weekView: Boolean,
    textMeasure: TextMeasurer
) {

    val days = "SMTWTFS"
    for (i in 0 until hoursView) {
        drawCircle(
            Color.Gray, 3f,
            Offset(startingOffset + (chartWidth / hoursView) * (i), chartHeight)
        )
        if (weekView) {
            val text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = Color.LightGray,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Normal
                    )
                ) {
                    append(days[i].toString())
                }
            }
            drawText(
                textMeasurer = textMeasure,
                text = text,
                topLeft = Offset(
                    startingOffset + (chartWidth / hoursView) * i - 5,
                    size.height - 20
                )
            )
        }
    }
    drawBottomDailyLegend(weekView, textMeasure)
    for (i in 1..8) {
        drawLine(
            Color.DarkGray,
            Offset(0f, 0f + (chartHeight / 9) * i),
            Offset(chartWidth, +(chartHeight / 9) * i),
            cap = StrokeCap.Round,
            strokeWidth = 0.5f
        )
    }
    if ( !weekView ) {
        for(i in 1 until 3) {
            drawLine(
                Color.DarkGray,
                Offset(i*(chartWidth/3)+6, 8f),
                Offset(i*(chartWidth/3)+6, chartHeight-8),
                cap = StrokeCap.Round,
                strokeWidth = 0.5f
            )
        }
    }
    drawLine(
        Color.LightGray,
        Offset(0f, 0f),
        Offset(chartWidth, 0f),
        cap = StrokeCap.Round,
        strokeWidth = 1.5f
    )
}


@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawBottomDailyLegend(
    weekView: Boolean,
    textMeasure: TextMeasurer
) {
    if (!weekView) {
        val text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = Color.LightGray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Normal
                )
            ) {
                append("12AM")
            }
        }
        val text2 = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = Color.LightGray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Normal
                )
            ) {
                append("12PM")
            }
        }
        val text3 = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = Color.LightGray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Normal
                )
            ) {
                append("6")
            }
        }
        drawText(
            textMeasurer = textMeasure,
            text = text,
            topLeft = Offset(0f, size.height - 20)
        )
        drawText(
            textMeasurer = textMeasure,
            text = text2,
            topLeft = Offset(size.width / 2 - 15, size.height - 20)
        )
        drawText(
            textMeasurer = textMeasure,
            text = text,
            topLeft = Offset(size.width - 45, size.height - 20)
        )
        drawText(
            textMeasurer = textMeasure,
            text = text3,
            topLeft = Offset(size.width * .25f+3, size.height - 20)
        )
        drawText(
            textMeasurer = textMeasure,
            text = text3,
            topLeft = Offset(size.width * .75f+2, size.height - 20)
        )
    }
}


private fun DrawScope.drawTodayLine(
    weekView: Boolean,
    startingOffset: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    if (weekView) {
        val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        val x = dow / 7f
        drawLine(
            Color.LightGray,
            Offset(startingOffset + x * chartWidth, 10f),
            Offset(startingOffset + x * chartWidth, chartHeight - 10),
            cap = StrokeCap.Round,
            strokeWidth = 2f
        )
    } else {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val x = (hour / 24f)// + (minute/60f)*(1/24f)
        drawLine(
            Color.LightGray,
            Offset(startingOffset + x * chartWidth, 10f),
            Offset(startingOffset + x * chartWidth, chartHeight - 10),
            cap = StrokeCap.Round,
            strokeWidth = 2f
        )
    }
}