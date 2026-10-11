package com.opesware.childtrace.parent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand + semantic severity palette, mirroring the design mockups.
private val Teal = Color(0xFF0E7A68)
private val TealDark = Color(0xFF3EC4AC)
val SevCritical = Color(0xFFC23526)
val SevHigh = Color(0xFFC9740A)
val SevMedium = Color(0xFF9A8405)
val SevOk = Color(0xFF0E7A68)

private val Light = lightColorScheme(primary = Teal, secondary = Teal)
private val Dark = darkColorScheme(primary = TealDark, secondary = TealDark)

@Composable
fun ChildTraceParentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}

/** Maps a severity to its chip color, consistent with the design system. */
fun severityColor(rank: Int): Color = when (rank) {
    4 -> SevCritical
    3 -> SevHigh
    2 -> SevMedium
    else -> SevOk
}
