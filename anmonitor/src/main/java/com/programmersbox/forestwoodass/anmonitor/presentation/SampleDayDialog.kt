package com.programmersbox.forestwoodass.anmonitor.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import com.programmersbox.forestwoodass.anmonitor.presentation.theme.WearAppTheme

class SampleDayDialog : ComponentActivity() {

    private var dow = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dow = intent?.getIntExtra("DOW", -1)!!
        setContent {
            WearApp()
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    fun WearApp() {
        WearAppTheme {
            Column(Modifier.fillMaxSize()) {
                val pagerState = rememberPagerState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    VerticalPager(
                        count = 1,
                        state = pagerState,
                    ) {
                            page ->
                        when ( page ) {
                            0 -> SamplingHistory(false, dow)
                        }
                    }
                }
            }
        }
    }
}