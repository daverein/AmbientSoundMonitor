package com.programmersbox.forestwoodass.anmonitor.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val Red400 = Color(0xFFCF6679)


fun getWearColorPalette(): Colors {
    return Colors(
        primary = Color.White,
        primaryVariant = Color.White,
        secondary = Color(android.graphics.Color.parseColor("#00E676")),
        secondaryVariant = Color( android.graphics.Color.parseColor(
            "#226949"
        )),
        error = Red400,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onError = Color.Black)
}
