package com.nosnooze.alarm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF171A1F)
val Paper = Color(0xFFFFF9F0)
val Orange = Color(0xFFFF6B35)
val Mint = Color(0xFFBCE8D1)
val Lavender = Color(0xFFDCCBFF)
val Sky = Color(0xFFC9E8FF)
val Muted = Color(0xFF6F7378)

private val scheme = lightColorScheme(
    primary = Ink, onPrimary = Color.White,
    secondary = Orange, onSecondary = Color.White,
    tertiary = Color(0xFF39735A),
    background = Paper, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFF1ECE4), onSurfaceVariant = Muted,
    outline = Color(0xFFD8D1C7), error = Color(0xFFBA1A1A)
)

@Composable fun NoSnoozeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
