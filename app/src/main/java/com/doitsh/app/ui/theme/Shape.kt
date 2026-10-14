package com.doitsh.app.ui.theme

import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes

/**
 * The Expressive eight-role corner scale.
 *
 * `ShapeDefaults` already exposes every Expressive role, and material3's
 * `Shapes()` default constructor is built from exactly these tokens — so this is
 * spelled out only to make the dependency explicit and to give feature code a
 * named handle. Values: ExtraSmall 4dp, Small 8dp, Medium 12dp, Large 16dp,
 * LargeIncreased 20dp, ExtraLarge 28dp, ExtraLargeIncreased 32dp,
 * ExtraExtraLarge 48dp.
 */
val ExpressiveShapes = Shapes(
    extraSmall = ShapeDefaults.ExtraSmall,
    small = ShapeDefaults.Small,
    medium = ShapeDefaults.Medium,
    large = ShapeDefaults.Large,
    extraLarge = ShapeDefaults.ExtraLarge,
    largeIncreased = ShapeDefaults.LargeIncreased,
    extraLargeIncreased = ShapeDefaults.ExtraLargeIncreased,
    extraExtraLarge = ShapeDefaults.ExtraExtraLarge,
)
