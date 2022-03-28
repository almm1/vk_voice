package com.example.vk_voice.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable


private val LightColorPalette = lightColors(
    primary = Blue,
    primaryVariant = Blue800,
    secondary = Grey,
    secondaryVariant = SemiWhite
)

@Composable
fun Vk_voiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = LightColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}