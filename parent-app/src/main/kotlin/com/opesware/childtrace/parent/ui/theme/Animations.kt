package com.opesware.childtrace.parent.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Motion helpers implementing docs/styling-spec.md §8. Everything here degrades
 * to a static value when the user has turned animations off.
 */

/** True when the system "remove animations" / animator-duration-scale = 0 is set. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * The single sanctioned glow: a gentle alpha pulse for a critical indicator.
 * Returns a steady alpha (no animation) under reduced motion.
 */
@Composable
fun criticalPulseAlpha(): Float {
    if (rememberReducedMotion()) return 0.9f
    val transition = rememberInfiniteTransition(label = "critical-pulse")
    return transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.pulse / 2, easing = Motion.standardEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "critical-pulse-alpha",
    ).value
}
