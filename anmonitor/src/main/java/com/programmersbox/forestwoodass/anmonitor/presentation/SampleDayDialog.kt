package com.programmersbox.forestwoodass.anmonitor.presentation

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.*
import com.programmersbox.forestwoodass.anmonitor.presentation.theme.WearAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SampleDayDialog : ComponentActivity() {
    private var serviceScope = CoroutineScope(Dispatchers.Main )
    private val dow: Int by lazy { intent?.getIntExtra("DOW", -1)!! }
    // Timestamp is the beginning of the week, on Sunday at 12:00am.
    private val timestamp: Long by lazy { intent?.getLongExtra("TIMESTAMP", 0L)!! }
    private val oneDay = (1000*60*60*24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SampleDayDialog", "Start DOW: $dow for timestamp is: ${DateFormat.format("MMM dd hh:mm:ss", timestamp)}")
        setContent {
            WearApp()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun WearApp() {
        val pagerState = rememberPagerState()
        WearAppTheme {
            Column(Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    VerticalPager(
                        count = 7,
                        state = pagerState,
                    ) {
                            page ->
                        when ( page ) {
                            0 -> SamplingHistory(false, -1, timestamp)
                            1 -> SamplingHistory(false, -1, timestamp+oneDay)
                            2 -> SamplingHistory(false, -1, timestamp+oneDay*2)
                            3 -> SamplingHistory(false, -1, timestamp+oneDay*3)
                            4 -> SamplingHistory(false, -1, timestamp+oneDay*4)
                            5 -> SamplingHistory(false, -1, timestamp+oneDay*5)
                            6 -> SamplingHistory(false, -1, timestamp+oneDay*6)
                        }
                    }
                }
            }
        }
        LaunchedEffect(key1 = 0, block = {
                serviceScope.launch {
                pagerState.scrollToPage(dow-1, 0f)
            }
        })

    }
}