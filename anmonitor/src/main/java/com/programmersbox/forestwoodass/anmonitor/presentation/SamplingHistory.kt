package com.programmersbox.forestwoodass.anmonitor.presentation

import android.graphics.*
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.google.accompanist.pager.*
import com.programmersbox.forestwoodass.anmonitor.data.repository.DBLevelStore
import com.programmersbox.forestwoodass.anmonitor.utils.COLORS_RED_START
import com.programmersbox.forestwoodass.anmonitor.utils.COLORS_YELLOW_START
import com.programmersbox.forestwoodass.anmonitor.utils.FormatTimeText
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.max
import kotlin.math.min

private const val TAG = "SamplingHistory"
private const val GraphColor = "#88888888"

@Composable
fun SamplingHistory(weekView: Boolean) {
    val dbHelper = DBLevelStore(LocalContext.current)
    val samples = dbHelper.getAllSamples(weekView)
    Log.d(TAG, "Got ${samples.size} items")
    var minValue = 120f
    var maxValue = 0f
    samples.forEach {
        minValue = min(minValue, it.sampleValue)
        maxValue = max(maxValue, it.sampleValue)
    }
    FormatTimeText(weekView)
    Column(
        modifier = Modifier
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.padding(12.dp))
        Text(buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 12.sp, color = Color.LightGray)) {
                if (weekView) {
                    append("This Week")
                } else {
                    append("Today")
                }
            }
        })
        DrawSampleRange(minValue, maxValue)
        Spacer(Modifier.padding(1.dp))
        ChartLevels(weekView, samples)
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
fun ChartLevels(weekView: Boolean, samples: ArrayList<DBLevelStore.SampleValue>) {
    val hoursView = when (weekView) {
        true -> 7
        false -> 24
    }
    val textMeasure = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .padding(PaddingValues(15.dp, 0.dp))
            .background(MaterialTheme.colors.background)
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


        drawTodayLine(weekView, startingOffset, chartWidth, chartHeight)

        Log.d(DBMonitor.TAG, "Got ${samples.size} items")
        if ( !weekView ) {
            drawDailyChart(samples, startingOffset, chartWidth, chartHeight)
        } else {
            drawWeeklyChart(samples, startingOffset, chartWidth, chartHeight)
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
    samples.forEach {
        cal.timeInMillis = it.timestamp
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1

        if (dow != dayCurrent) {
            if (dayCurrent != -1) {
                drawLine(
                    Color(android.graphics.Color.parseColor(GraphColor)),
                    Offset(
                        startingOffset + (dayCurrent / 7f) * chartWidth,
                        chartHeight - (dayMin / 90f) * chartHeight
                    ),
                    Offset(
                        startingOffset + (dayCurrent / 7f) * chartWidth,
                        chartHeight - (dayMax / 90f) * chartHeight
                    ),
                    cap = StrokeCap.Round,
                    strokeWidth = 8f
                )
            }
            dayCurrent = dow
            dayMin = 99f
            dayMax = 0f
        }
        val x = (dow / 7f)
        val sampleValue = it.sampleValue - 10f
        Log.d(
            DBMonitor.TAG,
            "sample ${it.sampleValue} / ${it.timestamp} taken $dow  $x"
        )
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
    drawLine(
        Color(android.graphics.Color.parseColor(GraphColor)),
        Offset(
            startingOffset + (dayCurrent / 7f) * chartWidth,
            chartHeight - (dayMin / 90f) * chartHeight
        ),
        Offset(
            startingOffset + (dayCurrent / 7f) * chartWidth,
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
        val minute = cal.get(Calendar.MINUTE)

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
        Log.d(
            DBMonitor.TAG,
            "sample ${it.sampleValue} / ${it.timestamp} taken $hour / $minute  $x"
        )
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