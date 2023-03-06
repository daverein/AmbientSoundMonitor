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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.*
import com.programmersbox.forestwoodass.anmonitor.presentation.theme.WearAppTheme

class SampleWeekDialog : ComponentActivity() {
    private val dow: Int by lazy { intent?.getIntExtra("DOW", 0)!! }
    private val timestamp: Long by lazy { intent?.getLongExtra("TIMESTAMP", 0L)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SampleWeekDialog", "Start DOW: $dow for timestamp is: ${DateFormat.format("MMM dd hh:mm:ss", timestamp)}")
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
                    HorizontalPager(
                        count = 4,
                        state = pagerState,
                    ) {
                            page ->
                        when ( page ) {
                            0 -> SamplingHistory(true, timestamp = timestamp)
                            1 -> SamplingHistory(true, timestamp = timestamp-(1000*60*60*24*7))
                            2 -> SamplingHistory(true, timestamp = timestamp-(1000*60*60*24*14))
                            3 -> SamplingHistory(true, timestamp = timestamp-(1000*60*60*24*21))
                        }
                    }
                }
            }
        }
    }
}