package com.programmersbox.forestwoodass.anmonitor.presentation

import android.annotation.SuppressLint
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
import com.programmersbox.forestwoodass.anmonitor.utils.MonitorDBLevels
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val TAG = "SamplingHistory"
private const val GraphColor = "#88888888"

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SamplingHistory(weekView: Boolean, timestamp:Long = 0L) {
    val viewCalendar = Calendar.getInstance()
    if ( timestamp != 0L ) {
        viewCalendar.timeInMillis = timestamp
    }
    val pagerState = rememberPagerState()
    if ( weekView ) {
        val viewCalendar2 = viewCalendar.clone() as Calendar
        val viewCalendar3 = viewCalendar.clone() as Calendar
        val viewCalendar4 = viewCalendar.clone() as Calendar
        viewCalendar2.timeInMillis -= (1000*60*60*24*7)
        viewCalendar3.timeInMillis -= (1000*60*60*24*7*2)
        viewCalendar4.timeInMillis -= (1000*60*60*24*7*3)
        HorizontalPager(
            count = 4,
            state = pagerState,
        ) { page ->
            when (page) {
                0 -> DrawBody(true, viewCalendar)
                1 -> DrawBody(true, viewCalendar2)
                2 -> DrawBody(true, viewCalendar3)
                3 -> DrawBody(true, viewCalendar4)
            }
        }
    } else {
        DrawBody(false, viewCalendar)
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
private fun DrawBody(
    weekView: Boolean,
    viewCalendar: Calendar
) {
    val dbHelper = DBLevelStore(LocalContext.current)

    val samples by remember { mutableStateOf (dbHelper.getAllSamples(weekView, viewCalendar.timeInMillis))}
    var minValue = 120f
    var maxValue = 0f
    samples.forEach {
        minValue = min(minValue, it.sampleValue)
        maxValue = max(maxValue, it.sampleValue)
    }

    FormatTimeText(weekView, viewCalendar.timeInMillis)

    Column(
        modifier = Modifier
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.padding(12.dp))
        DrawChartTitle(weekView, viewCalendar)
        DrawSampleRange(minValue, maxValue, samples)
        Spacer(Modifier.padding(1.dp))
        ChartLevels(weekView, viewCalendar, samples)
        Spacer(Modifier.padding(2.dp))
        DrawSummaryInfo(samples)
    }
}

@Composable
private fun DrawSummaryInfo(samples: java.util.ArrayList<DBLevelStore.SampleValue>) {
    val monitor = MonitorDBLevels(LocalContext.current, null)
    var warning = false
    MonitorDBLevels.DbDoseLength.values().forEach {

        val timeAbove = monitor.minutesInRange(it.dbLevel, 120f, samples)
        if ( timeAbove > 0 ) {
            warning = true
            Text(buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        color = it.warningColor
                    )
                ) {
                    append(">= ${it.dbLevel}dB: ${timeAbove / (60 * 60)}h, ${timeAbove / (60) % 60}m")
                }
            })
        }
    }
    if ( !warning ) {
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
    calIn: Calendar,
) {
    val currentDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val calInDayOfYear = calIn.get(Calendar.DAY_OF_YEAR)
    Text(buildAnnotatedString {
        withStyle(style = SpanStyle(fontSize = 12.sp, color = Color.LightGray)) {
            if (weekView) {
                if ( calInDayOfYear + 7 > currentDayOfYear) {
                    append("This Week")
                } else if ( calInDayOfYear + 14 > currentDayOfYear ) {
                    append("Last Week")
                } else if ( calInDayOfYear + 21 > currentDayOfYear ) {
                    append("2 weeks ago")
                } else {
                    append("3 weeks ago")
                }
            } else if (calIn.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) &&
                    calIn.get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
                    append("Today")
            } else {
                append("${DateFormat.format("EEE MMM dd", calIn)}")
            }
        }
    })
}

@Composable
private fun DrawSampleRange(
    minValue: Float,
    maxValue: Float,
    samples: ArrayList<DBLevelStore.SampleValue>
) {
    if ( samples.size == 0 ) {
        Text(buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 21.sp, color = MaterialTheme.colors.primary)) {
                append("No data")

            }
        })
    } else {
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
}


@OptIn(ExperimentalTextApi::class)
@Composable
fun ChartLevels(
    weekView: Boolean,
    viewCalendar: Calendar,
    samples: ArrayList<DBLevelStore.SampleValue>
) {
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
                        if ( weekView ) {
                            val cal = viewCalendar.clone() as Calendar
                            val mDow = floor((tapOffset.x /size.width.toFloat())*7).toInt()+1
                            val startWeekTime = cal.timeInMillis -
                                    ((cal.get(Calendar.DAY_OF_WEEK) - 1) *
                                            (1000 * 60 * 60 * 24))
                            Log.d(TAG, "Tapped offset = ${tapOffset.x}  days back = $mDow, with ${DateFormat.format(
                                "MMM dd hh:mm:ss",
                                startWeekTime
                            )}")
                            context.startActivity(
                                Intent(context, SampleDayDialog::class.java)
                                    .putExtra("DOW", mDow)
                                    .putExtra("TIMESTAMP", startWeekTime)
                            )
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

        if ( viewCalendar.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH) &&
            viewCalendar.get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) ) {
            drawTodayLine(weekView, startingOffset, chartWidth, chartHeight)
        }

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

    if ( samples.size > 0 ) {
        val x = (dayCurrent / 7f) + (dayPart * (1 / 21f))
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
    if ( samples.size > 0 ) {
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