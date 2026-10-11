package com.opesware.childtrace.design.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Non-color design tokens from docs/styling-spec.md. Screens read these instead
 * of hardcoding dp/sp, so spacing, radii, icon sizes and motion stay consistent
 * and are changed in one place.
 */

/** 4dp spacing grid. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp   // screen gutter, card padding
    val xl = 24.dp
    val xxl = 32.dp
}

/** Corner radii, one per role. */
object Radii {
    val sm = 8.dp    // chips, inputs
    val md = 12.dp   // cards
    val lg = 20.dp   // sheets, hero
}

/** Icon sizes; the touch target stays >= 48dp regardless. */
object IconSize {
    val sm = 16.dp
    val md = 20.dp
    val lg = 24.dp
    val xl = 40.dp
    val minTouchTarget = 48.dp
}

/** Elevation scale (dp). Nothing above dialog. */
object Elevation {
    val flat = 0.dp
    val card = 1.dp
    val bar = 3.dp
    val dialog = 8.dp
}

/** Motion durations (ms) and the standard easing. */
object Motion {
    const val fast = 120
    const val base = 220
    const val slow = 400
    const val pulse = 2400
    val standardEasing = FastOutSlowInEasing
}

/** Type sizes (sp) from the scale; paired with line heights in Typography. */
object TypeScale {
    val displayL = 30.sp
    val titleL = 22.sp
    val titleM = 18.sp
    val body = 16.sp
    val bodyS = 14.sp
    val label = 13.sp
    val caption = 12.sp
    val mono = 13.sp
}
