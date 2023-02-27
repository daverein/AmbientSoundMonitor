package com.programmersbox.forestwoodass.anmonitor.presentation.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import com.programmersbox.forestwoodass.anmonitor.data.repository.SamplingSoundDataRepository

val Red400 = Color(0xFFCF6679)


fun getWearColorPalette(context: Context): Colors {
    val repo = SamplingSoundDataRepository(context)
    return Colors(
        primary = Color.White,
        primaryVariant = Color.White,
        secondary = Color(repo.getSamplePrimaryColor()),
        secondaryVariant = Color(repo.getSampleSecondaryColor()),
        error = Red400,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onError = Color.Black)
}
