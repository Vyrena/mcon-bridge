package com.vyrena.mconbridge.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BridgeColors = darkColorScheme(
    primary = Color(0xFF9A83FF),
    onPrimary = Color(0xFF130A3C),
    primaryContainer = Color(0xFF2A2152),
    secondary = Color(0xFF55D6BE),
    background = Color(0xFF0B0D12),
    surface = Color(0xFF121722),
    surfaceVariant = Color(0xFF1A2030),
    onBackground = Color(0xFFF1EFFF),
    onSurface = Color(0xFFF1EFFF),
    error = Color(0xFFFF6B7D),
)

@Composable
fun BridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BridgeColors, content = content)
}
