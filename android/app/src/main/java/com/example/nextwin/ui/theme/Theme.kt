package com.example.nextwin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NexTwin Dark Cybersecurity Color Palette
private val NexTwinDarkColorScheme = darkColorScheme(

    // Primary electric blue
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF001827),

    // Secondary blue
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF101535),

    // Accent cyan
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF00252C),

    // Main backgrounds
    background = Color(0xFF080F1F),
    onBackground = Color(0xFFEAF2FF),

    // Cards and surfaces
    surface = Color(0xFF111D33),
    onSurface = Color(0xFFEAF2FF),

    surfaceVariant = Color(0xFF1B2B45),
    onSurfaceVariant = Color(0xFFB8C7DD),

    // Borders and dividers
    outline = Color(0xFF344966),

    // Error and malicious alerts
    error = Color(0xFFFF5263),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun NexTwinTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = NexTwinDarkColorScheme,
        typography = Typography,
        content = content
    )
}