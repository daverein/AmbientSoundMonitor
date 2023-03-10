package com.programmersbox.forestwoodass.anmonitor.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.*
import com.google.accompanist.pager.*
import com.programmersbox.forestwoodass.anmonitor.R
import com.programmersbox.forestwoodass.anmonitor.presentation.theme.WearAppTheme
import com.programmersbox.forestwoodass.anmonitor.services.SamplingService
import com.programmersbox.forestwoodass.anmonitor.utils.*
import kotlinx.coroutines.*
import kotlin.math.min


class DBMonitor : ComponentActivity() {
    private val soundRecord: SoundRecorder by lazy { SoundRecorder(this) }
    private val isRound: Boolean by lazy {
        resources.configuration.isScreenRound
    }

    data class DBValues(
        val maxDB: Float = 0f,
        val minDB: Float = 0f,
        val avgDB: Float = 0f
    )

    private var dbText by mutableStateOf(DBValues())
    private var isServiceStateActive: Boolean by mutableStateOf(false)
    private var isSamplingServiceStateActive: Boolean by mutableStateOf(false)
    private var levelIndicatorValue: Float by mutableStateOf(0f)
    private var myMinimumValue = 128.0

    private lateinit var runningService: Job
    private var serviceJob = Job()
    private var serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var runningUIUpdateService: Job
    private var serviceUIUpdateJob = Job()
    private var serviceUIUpdateScope = CoroutineScope(Dispatchers.Default + serviceUIUpdateJob)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startMonitor { dbText = it }
            isServiceStateActive = true
        } else {
            // PERMISSION NOT GRANTED
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun WearApp() {
        isSamplingServiceStateActive = isMyServiceRunning(this, SamplingService::class.java)
        WearAppTheme {
            Column(Modifier.fillMaxSize()) {
                val pagerState = rememberPagerState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    VerticalPager(
                        count = 4,
                        state = pagerState,
                    ) {
                            page ->
                        when ( page ) {
                            3 -> ServicePage()
                            2 -> SamplingHistory(true)
                            1 -> SamplingHistory(false)
                            0 -> Page0()
                        }
                    }
                }
            }
        }
    }




    @Composable
    fun Page0()
    {
        LevelIndicator(levelIndicatorValue)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            DBTextValue(text = dbText)
            ActionButton(
                onStateServiceChange = { isServiceStateActive = it },
                isStateServiceActive = isServiceStateActive
            )
        }
    }

    @Composable
    fun ServicePage()
    {
        FormatTimeText(false)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            Spacer(Modifier.padding(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = 14.sp, color = MaterialTheme.colors.secondary)) {
                        append("Ambient Background")
                    }
                }
            )
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = 14.sp, color = MaterialTheme.colors.secondary)) {
                        append("Service Status")
                    }
                }
            )

            Spacer(Modifier.padding(10.dp))
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = 14.sp, color = MaterialTheme.colors.primary, fontStyle = FontStyle.Italic)) {
                        append(when ( isSamplingServiceStateActive ) {
                            true -> "Running"
                            false -> "Not running"
                        })
                    }
                }
            )
            Spacer(Modifier.padding(15.dp))
            ServiceButton(
                onStateServiceChange = { isSamplingServiceStateActive = it },
                isStateServiceActive = isSamplingServiceStateActive
            )
        }
    }

    @Composable
    fun ServiceButton(
        onStateServiceChange: (Boolean) -> Unit,
        isStateServiceActive: Boolean
    ) {
        val context = LocalContext.current
        Button(
            modifier = Modifier
                .padding(vertical = 0.dp)
                .height(34.dp),
            enabled = true,
            onClick = {
                if ( isMyServiceRunning(context, SamplingService::class.java) ) {
                    try {
                        val sampleServiceIntent = Intent()
                        sampleServiceIntent.setPackage(SAMPLING_PACKAGE_NAME)
                        sampleServiceIntent.action = "com.programmersbox.forestwoodass.anmonitor.STOP"
                        sampleServiceIntent.addCategory("android.intent.category.DEFAULT")
                        context.startService(sampleServiceIntent)
                    } catch (ex: Exception) {
                        // Oops.
                    }
                    onStateServiceChange(false)
                } else {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) != PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        val sampleServiceIntent = Intent()
                        sampleServiceIntent.setPackage(SAMPLING_PACKAGE_NAME)
                        sampleServiceIntent.action = "com.programmersbox.forestwoodass.anmonitor.START"
                        sampleServiceIntent.addCategory(Intent.CATEGORY_DEFAULT)
                        context.startForegroundService(sampleServiceIntent)

                        Log.d(TAG, "checkToStartService: attempted to start foreground service")
                        onStateServiceChange(true)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                Color(MaterialTheme.colors.secondaryVariant.toArgb())
            ),
            content = {
                Text(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.primary,
                    text = if (isStateServiceActive) {
                        stringResource(R.string.sampledb_stop)
                    } else {
                        stringResource(R.string.sampledb_start)
                    }
                )
            }
        )
    }


    @Composable
    fun LevelIndicator(value: Float) {
        val colorStops = arrayOf(
            COLORS_GREEN_START to Color.Green,
            COLORS_YELLOW_START to Color.Yellow,
            COLORS_RED_START to Color.Red
        )
        val sweepLength = 250f
        val startAngle = -215f
        val valueAnimate by animateFloatAsState(
            targetValue = value,
            animationSpec = tween(ANIMATE_RATE_MS)
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            val strokeWidth = 17f
            val edgeOffset = 14f
            val ballRadius = 13f
            if ( isRound ) {
                val brush = Brush.linearGradient(colorStops = colorStops)
                scale(0.935f, center) {
                    drawArc(
                        brush, startAngle, sweepLength, false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                val position = sweepLength * (valueAnimate / 100f)
                rotate(startAngle + 90f + position, center) {
                    translate(0f, -size.height / 2f + edgeOffset) {
                        drawCircle(Color.White, ballRadius)
                    }
                }
            } else {
                val brush = Brush.horizontalGradient(colorStops = colorStops)
                val scaleOffset = 15f
                drawLine(
                    brush,
                    Offset(scaleOffset, edgeOffset),
                    Offset(size.width - scaleOffset, edgeOffset),
                    cap = StrokeCap.Round,
                    strokeWidth = strokeWidth
                )
                val position = (size.width - scaleOffset) * (valueAnimate / 100f)
                drawCircle(
                    Color.White,
                    ballRadius,
                    center = Offset(scaleOffset + position, edgeOffset)
                )
            }
        }
    }

    @Composable
    fun DBTextValue(text: DBValues) {
        if ( text.minDB.toDouble() > 10.0) {
            myMinimumValue = min(myMinimumValue, text.minDB.toDouble())
        }
        val minText = if ( myMinimumValue == 128.0 ) {
            "Min: ---"
        } else {
            String.format(
                "Min: %.1f",
                myMinimumValue)
        }
        Spacer(Modifier.padding(10.dp))
        Text(

            buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 14.sp, color = MaterialTheme.colors.secondary)) {
                    append(String.format(
                        "Max: %.1f",
                        text.maxDB))
                }
                withStyle(style = SpanStyle(fontSize = 10.sp, color = MaterialTheme.colors.secondary)) {
                    append(" dB")
                }
            }
        )
        Spacer(Modifier.padding(0.5.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 42.sp, color = MaterialTheme.colors.primary)) {
                    append(String.format(
                        "%.1f",
                        text.avgDB))
                }
                withStyle(style = SpanStyle(fontSize = 18.sp)) {
                    append(" dB")
                }
            }
        )
        Spacer(Modifier.padding(0.5.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 14.sp, color = MaterialTheme.colors.secondary)) {
                    append(minText)
                }
                withStyle(style = SpanStyle(fontSize = 10.sp, color = MaterialTheme.colors.secondary)) {
                    append(" dB")
                }
            }
        )

        Spacer(Modifier.padding(5.dp))
    }

    @Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreview() {
        WearApp()
    }


    @Composable
    fun ActionButton(
        onStateServiceChange: (Boolean) -> Unit,
        isStateServiceActive: Boolean
    ) {
        val context = LocalContext.current
        Button(
            modifier = Modifier
                .padding(vertical = 0.dp)
                .height(34.dp),
            enabled = true,
            onClick = {
                if (::runningService.isInitialized && runningService.isActive) {
                    runningService.cancel()
                    runningUIUpdateService.cancel()
                    onStateServiceChange(false)

                    val window = context.findActivity()?.window
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) != PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        startMonitor { dbText = it }
                        onStateServiceChange(true)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                Color(MaterialTheme.colors.secondaryVariant.toArgb())
            ),
            content = {
                Text(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.primary,
                    text = if (isStateServiceActive) {
                        stringResource(R.string.sampledb_stop)
                    } else {
                        stringResource(R.string.sampledb_measure)
                    }
                )
            }
        )
    }

    override fun onPause() {
        super.onPause()
        stopServices()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServices()
    }

    private fun stopServices() {
        if (::runningService.isInitialized) {
            runningService.cancel()
            runningUIUpdateService.cancel()
        }
        val window = findActivity()?.window
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    @SuppressLint("MissingPermission")
    private fun startMonitor(
        onDbTextStateChange: (DBValues) -> Unit
    ) {
        runningService = serviceScope.launch {
            soundRecord.record()
        }
        runningUIUpdateService = serviceUIUpdateScope.launch {
            withContext(Dispatchers.Main) {
                val window = findActivity()?.window
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            while (runningUIUpdateService.isActive) {
                delay(UPDATE_RATE_MS)
                withContext(Dispatchers.Main) {
                    onDbTextStateChange(
                        DBValues(
                            maxDB = soundRecord.liveMaxDB.toFloat(),
                            minDB = soundRecord.liveMinDB.toFloat(),
                            avgDB = soundRecord.liveAvgDB.toFloat()
                        )
                    )
                    levelIndicatorValue = soundRecord.liveAvgDB.toFloat()
                }
            }
        }
    }

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    companion object {
        const val TAG = "DBMonitor"
        const val UPDATE_RATE_MS = 333L
        const val ANIMATE_RATE_MS = (UPDATE_RATE_MS.toInt()-50)
        private const val SAMPLING_PACKAGE_NAME = "com.programmersbox.forestwoodass.anmonitor"
    }
}


