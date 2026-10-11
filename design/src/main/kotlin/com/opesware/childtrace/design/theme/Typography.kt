package com.opesware.childtrace.design.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The type scale from docs/styling-spec.md mapped onto Material 3 text styles.
 *
 * Sizes are in sp, so they honor the user's font-scale setting. The three
 * families (Fraunces display, Public Sans body, IBM Plex Mono data) are wired
 * as downloadable Google Fonts in a follow-up; until then these fall back to
 * the platform families but keep the exact sizes, weights and line heights, so
 * layouts are already built to the final metrics.
 */
private val Display = FontFamily.Default // TODO(assets): Fraunces
private val Body = FontFamily.Default     // TODO(assets): Public Sans
private val Mono = FontFamily.Monospace   // TODO(assets): IBM Plex Mono

fun childTraceTypography(): Typography = Typography(
    // Display hero (rare)
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    // Screen title
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    // Section / card title
    titleMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    // Default body
    bodyLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    // Secondary
    bodyMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    // Buttons, chips, nav
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    // Caption / timestamp
    bodySmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    // Data / mono (counts, codes)
    labelMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
)

/** Foreground color for a severity rank (its paired container comes from [severityColors]). */
@Composable
fun severityColor(rank: Int): Color = LocalChildTraceColors.current.severity(rank).first
