package com.doitsh.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// ─── M3 Expressive Shape Scale ─────────────────────────────────
object ShapeTokens {
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 28.dp
}

// ─── Capsule / Pill Shape ───────────────────────────────────────
val CapsuleShape: Shape = RoundedCornerShape(50)

// ─── Squircle (Superellipse) Shape ──────────────────────────────
// Creates continuous-curvature corners (iOS-style) for M3 Expressive.
// Unlike RoundedCornerShape (circular arc), the squircle has a smoother
// transition between the flat edge and the curved corner.
//
// Usage: Modifier.clip(SquircleShape(16.dp)) or as shape parameter in Card etc.
class SquircleShape(private val cornerRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val rPx = with(density) { cornerRadius.toPx() }
        val r = rPx.coerceIn(0f, minOf(size.width, size.height) / 2f)
        if (r <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        return Outline.Generic(buildSquirclePath(size, r))
    }
}

private fun buildSquirclePath(size: Size, r: Float): Path {
    val w = size.width
    val h = size.height
    // Control point distance: ~1.26× the standard circle kappa (0.5523)
    // This creates the characteristic "stretchy" squircle curvature
    val cp = r * 0.7f

    return Path().apply {
        // Start at top center
        moveTo(w / 2f, 0f)

        // Top edge → top-right squircle corner
        lineTo(w - r, 0f)
        cubicTo(w - r + cp, 0f, w, r - cp, w, r)

        // Right edge → bottom-right squircle corner
        lineTo(w, h - r)
        cubicTo(w, h - r + cp, w - r + cp, h, w - r, h)

        // Bottom edge → bottom-left squircle corner
        lineTo(r, h)
        cubicTo(r - cp, h, 0f, h - r + cp, 0f, h - r)

        // Left edge → top-left squircle corner
        lineTo(0f, r)
        cubicTo(0f, r - cp, r - cp, 0f, r, 0f)

        close()
    }
}

// ─── M3 Shapes ──────────────────────────────────────────────────
// Use RoundedCornerShape for the built-in M3 shapes (required by Shapes constructor).
// Apply SquircleShape directly via Modifier.clip() or shape parameter where desired.
val DoitshShapes = Shapes(
    extraSmall = RoundedCornerShape(ShapeTokens.ExtraSmall),
    small = RoundedCornerShape(ShapeTokens.Small),
    medium = RoundedCornerShape(ShapeTokens.Medium),
    large = RoundedCornerShape(ShapeTokens.Large),
    extraLarge = RoundedCornerShape(ShapeTokens.ExtraLarge),
)
