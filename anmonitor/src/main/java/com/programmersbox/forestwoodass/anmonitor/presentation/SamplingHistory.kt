package com.programmersbox.forestwoodass.anmonitor.presentation


import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material.*
import com.google.accompanist.pager.*
import com.programmersbox.forestwoodass.anmonitor.data.repository.DBLevelStore
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.max
import kotlin.math.min


const val TAG = "SamplingHistory"

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SamplingHistory(pagerState: PagerState) {
    val dbHelper = DBLevelStore(LocalContext.current)
    val samples = dbHelper.getAllSamples(1000*60*60*24)
    Log.d(TAG, "Got ${samples.size} items")
    var minValue = 120f
    var maxValue = 0f
    samples.forEach {
        minValue = min(minValue, it.sampleValue)
        maxValue = max(maxValue, it.sampleValue)
    }
    val leadingTextStyle = TimeTextDefaults.timeTextStyle(color = MaterialTheme.colors.primary, fontSize = 8.sp)
    val leadingTextStyle2 = TimeTextDefaults.timeTextStyle(color = MaterialTheme.colors.secondary, fontSize = 8.sp)
    val month :String = DateFormat.format("MMM", Calendar.getInstance().timeInMillis).toString()
    val day :String = DateFormat.format(" dd", Calendar.getInstance().timeInMillis).toString()

    TimeText(
        timeSource = TimeTextDefaults.timeSource(
            DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm")
        ),
        timeTextStyle = leadingTextStyle,
        startLinearContent = {
            Text(
                text = "ETA 12:48",
                style = leadingTextStyle2
            )
        },
        startCurvedContent = {
            curvedText(
                text = month,
                style = CurvedTextStyle(leadingTextStyle2)
            )
            curvedText(
                text = day,
                style = CurvedTextStyle(leadingTextStyle2)
            )
        },
    )
    Column(
    modifier = Modifier
    .selectableGroup(),
    horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Spacer(Modifier.padding(10.dp))
        Text(buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 10.sp, color = Color.LightGray)) {
                append("Today")
            }
        })
        Spacer(Modifier.padding(1.dp))
        Text(buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 18.sp, color = MaterialTheme.colors.primary)) {
                append(String.format(
                    "%.1f",
                    minValue))
            }
            withStyle(style = SpanStyle(fontSize = 8.sp, color = MaterialTheme.colors.primary)) {
                append("dB")
            }
            withStyle(style = SpanStyle(fontSize = 18.sp, color = MaterialTheme.colors.primary)) {
                append(String.format(
                    " - %.1f",
                    maxValue))
            }
            withStyle(style = SpanStyle(fontSize = 8.sp, color = MaterialTheme.colors.primary)) {
                append("dB")
            }
        })
        Spacer(Modifier.padding(3.dp))
        ChartLevels()
        Spacer(Modifier.padding(3.dp))
        Text(buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 12.sp, color = MaterialTheme.colors.secondaryVariant)) {
                append("No warnings")
            }
        })
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun ChartLevels()
{
    val hoursView = 24
    val dbHelper = DBLevelStore(LocalContext.current)
    val textMeasure = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(PaddingValues(15.dp, 0.dp))
            .background(MaterialTheme.colors.background)
    ) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 8f), 0f)
        val chartHeight = size.height - 25
        val chartWidth = size.width
        val colorStops = arrayOf(
            0.0f to Color.Red,
            0.10f to Color.Yellow,
            0.99f to Color.Green
        )
        val brush = Brush.verticalGradient(colorStops = colorStops)
        drawRect(brush, alpha = 0.2f, topLeft = Offset(0f,0f), size = Size(chartWidth, chartHeight))

        for ( i in 0..hoursView ) {
            drawCircle(Color.Gray, 3f,
                Offset(0f + (chartWidth/hoursView)*i, chartHeight))

            val text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = Color.LightGray,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.ExtraLight,
                        fontStyle = FontStyle.Normal
                    )
                ) {
                    append("12 AM")
                }
            }
            val text2 = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = Color.LightGray,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.ExtraLight,
                        fontStyle = FontStyle.Normal
                    )
                ) {
                    append("12 PM")
                }
            }
            val text3 = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = Color.LightGray,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.ExtraLight,
                        fontStyle = FontStyle.Normal
                    )
                ) {
                    append("6")
                }
            }
            drawText(
                textMeasurer = textMeasure,
                text = text,
                topLeft = Offset(0f, size.height-20)
            )
            drawText(
                textMeasurer = textMeasure,
                text = text2,
                topLeft = Offset(size.width/2-15, size.height-20)
            )
            drawText(
                textMeasurer = textMeasure,
                text = text,
                topLeft = Offset(size.width-35, size.height-20)
            )
            drawText(
                textMeasurer = textMeasure,
                text = text3,
                topLeft = Offset(size.width*.25f-3, size.height-20)
            )
            drawText(
                textMeasurer = textMeasure,
                text = text3,
                topLeft = Offset(size.width*.75f-3, size.height-20)
            )
        }
        for ( i in 1..8) {
            drawLine(
                Color.LightGray,
                Offset(0f , 0f+ (chartHeight/9)*i),
                Offset(chartWidth, + (chartHeight/9)*i),
                cap = StrokeCap.Round,
                strokeWidth = 0.1f,
                pathEffect = pathEffect
            )
        }
        drawLine(
            Color.DarkGray,
            Offset(0f, 0f),
            Offset(chartWidth, 0f),
            cap = StrokeCap.Round,
            strokeWidth = 1f
        )

        val rc = dbHelper.getAllSamples(1000*60*60*hoursView)
        Log.d(DBMonitor.TAG, "Got ${rc.size} items")
        val cal = Calendar.getInstance()
        var minValue = 120f
        var maxValue = 0f
        var hourMin = 99f
        var hourMax = 0f
        var hourCurrent = -1
        rc.forEach {
            cal.timeInMillis = it.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            if ( hour != hourCurrent ) {
                if ( hourCurrent != -1 ) {
                    drawLine(
                        Color(android.graphics.Color.parseColor("#88888888")),
                        Offset((hourCurrent / 24f) * chartWidth, chartHeight - (hourMin/90f)*chartHeight),
                        Offset((hourCurrent / 24f) * chartWidth, chartHeight - (hourMax/90f)*chartHeight),
                        cap = StrokeCap.Round,
                        strokeWidth = 8f
                    )
                }
                hourCurrent = hour
                hourMin = 99f
                hourMax = 0f
            }
            val x = (hour/24f)
            val sampleValue = it.sampleValue - 10f
            Log.d(DBMonitor.TAG, "sample ${it.sampleValue} / ${it.timestamp} taken $hour / $minute  $x")
            drawCircle(Color.Green, 4f,
                Offset(0f + (chartWidth*(x)), chartHeight - (sampleValue/90f)*chartHeight))
            minValue = min(minValue, it.sampleValue)
            maxValue = max(maxValue, it.sampleValue)
            hourMin = min(hourMin, sampleValue)
            hourMax = max(hourMax, sampleValue)
        }
        drawLine(
            Color(android.graphics.Color.parseColor("#88888888")),
            Offset((hourCurrent / 24f) * chartWidth, chartHeight - (hourMin/90f)*chartHeight),
            Offset((hourCurrent / 24f) * chartWidth, chartHeight - (hourMax/90f)*chartHeight),
            cap = StrokeCap.Round,
            strokeWidth = 6f
        )

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val x = (hour/24f)// + (minute/60f)*(1/24f)
        drawLine(
            Color.LightGray,
            Offset(x*chartWidth, 10f),
            Offset(x*chartWidth, chartHeight-10),
            cap = StrokeCap.Round,
            strokeWidth = 2f
        )
    }
}