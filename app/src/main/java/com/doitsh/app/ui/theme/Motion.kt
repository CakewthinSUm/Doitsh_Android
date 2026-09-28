package com.doitsh.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec

// ─── Spring Physics Tokens (M3 Expressive) ──────────────────────
// Spring-first approach: all component transitions use springs by default.
object MotionTokens {

    // Default spring — slight overshoot, balanced feel
    val DefaultSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    // Snappy spring — more overshoot, high responsiveness
    val SnappySpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    // Gentle spring — soft, less overshoot
    val GentleSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessLow,
    )

    // Sticky spring — no overshoot, smooth settle
    val StickySpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    // ─── Emphasized Easing (M3 Expressive signature curve) ──────
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // ─── Duration Tokens (for choreographed sequences) ──────────
    const val DurationShortMs: Int = 200
    const val DurationMediumMs: Int = 350
    const val DurationLongMs: Int = 500

    // ─── Choreographed entry delays (staggered list items) ──────
    const val StaggerDelayMs: Int = 50
}
