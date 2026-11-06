package com.carlomontec.volumefader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = DeepBlack,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    background = DeepBlack,
    onBackground = TextPrimary,
    error = AccentRed
)

@Composable
fun VolumeFaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
