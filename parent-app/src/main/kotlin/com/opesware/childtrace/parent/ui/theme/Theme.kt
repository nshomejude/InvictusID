package com.opesware.childtrace.parent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- Palette (docs/styling-spec.md §1) ---
private object Light {
    val bg = Color(0xFFF4F6F6)
    val surface = Color(0xFFFFFFFF)
    val surfaceAlt = Color(0xFFEDF1F0)
    val ink = Color(0xFF11201D)
    val inkSoft = Color(0xFF54635F)
    val inkFaint = Color(0xFF8A9793)
    val line = Color(0xFFDCE3E1)
    val accent = Color(0xFF0E7A68)
    val onAccent = Color(0xFFFFFFFF)
    val accentSoft = Color(0xFFD7ECE6)
    val critical = Color(0xFFC23526); val criticalBg = Color(0xFFFAE7E4)
    val high = Color(0xFFC9740A); val highBg = Color(0xFFFBEED8)
    val medium = Color(0xFF9A8405); val mediumBg = Color(0xFFF5F0D0)
    val safe = Color(0xFF0E7A68); val safeBg = Color(0xFFD7ECE6)
}

private object Dark {
    val bg = Color(0xFF0A1211)
    val surface = Color(0xFF13201D)
    val surfaceAlt = Color(0xFF0F1A18)
    val ink = Color(0xFFE9F1EF)
    val inkSoft = Color(0xFF9FB0AB)
    val inkFaint = Color(0xFF6B7D78)
    val line = Color(0xFF243330)
    val accent = Color(0xFF3EC4AC)
    val onAccent = Color(0xFF04201A)
    val accentSoft = Color(0xFF14332D)
    val critical = Color(0xFFFF7063); val criticalBg = Color(0xFF341915)
    val high = Color(0xFFF0A63F); val highBg = Color(0xFF33240F)
    val medium = Color(0xFFDDC74A); val mediumBg = Color(0xFF2A2710)
    val safe = Color(0xFF3EC4AC); val safeBg = Color(0xFF14332D)
}

/**
 * Tokens Material 3's ColorScheme does not carry: the ink hierarchy and the
 * severity family with their paired containers. Exposed via [LocalChildTraceColors]
 * so a severity color is always drawn with its own background (anti-collision).
 */
@Immutable
data class ChildTraceColors(
    val bg: Color, val surface: Color, val surfaceAlt: Color,
    val ink: Color, val inkSoft: Color, val inkFaint: Color, val line: Color,
    val accentSoft: Color,
    val critical: Color, val criticalBg: Color,
    val high: Color, val highBg: Color,
    val medium: Color, val mediumBg: Color,
    val safe: Color, val safeBg: Color,
) {
    /** The (foreground, background) pair for a severity rank — never used apart. */
    fun severity(rank: Int): Pair<Color, Color> = when (rank) {
        4 -> critical to criticalBg
        3 -> high to highBg
        2 -> medium to mediumBg
        else -> safe to safeBg
    }
}

val LocalChildTraceColors = staticCompositionLocalOf {
    // Safe light default; replaced by the theme.
    with(Light) {
        ChildTraceColors(bg, surface, surfaceAlt, ink, inkSoft, inkFaint, line, accentSoft,
            critical, criticalBg, high, highBg, medium, mediumBg, safe, safeBg)
    }
}

private val LightScheme = lightColorScheme(
    primary = Light.accent, onPrimary = Light.onAccent,
    primaryContainer = Light.accentSoft, onPrimaryContainer = Light.ink,
    background = Light.bg, onBackground = Light.ink,
    surface = Light.surface, onSurface = Light.ink,
    surfaceVariant = Light.surfaceAlt, onSurfaceVariant = Light.inkSoft,
    outline = Light.line, error = Light.critical, onError = Light.onAccent,
)

private val DarkScheme = darkColorScheme(
    primary = Dark.accent, onPrimary = Dark.onAccent,
    primaryContainer = Dark.accentSoft, onPrimaryContainer = Dark.ink,
    background = Dark.bg, onBackground = Dark.ink,
    surface = Dark.surface, onSurface = Dark.ink,
    surfaceVariant = Dark.surfaceAlt, onSurfaceVariant = Dark.inkSoft,
    outline = Dark.line, error = Dark.critical, onError = Dark.onAccent,
)

@Composable
fun ChildTraceParentTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extended = if (dark) {
        with(Dark) {
            ChildTraceColors(bg, surface, surfaceAlt, ink, inkSoft, inkFaint, line, accentSoft,
                critical, criticalBg, high, highBg, medium, mediumBg, safe, safeBg)
        }
    } else {
        with(Light) {
            ChildTraceColors(bg, surface, surfaceAlt, ink, inkSoft, inkFaint, line, accentSoft,
                critical, criticalBg, high, highBg, medium, mediumBg, safe, safeBg)
        }
    }
    CompositionLocalProvider(LocalChildTraceColors provides extended) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = childTraceTypography(),
            content = content,
        )
    }
}

/** Convenience for the severity chip color used across the console. */
@Composable
fun severityColors(rank: Int): Pair<Color, Color> = LocalChildTraceColors.current.severity(rank)
