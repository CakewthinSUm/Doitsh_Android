package com.doitsh.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─── Primary (Blue) ──────────────────────────────────────────────
val Blue100 = Color(0xFFD6E3FF)
val Blue90 = Color(0xFFAAC7FF)
val Blue80 = Color(0xFF7EB3FF)
val Blue70 = Color(0xFF539BFF)
val Blue40 = Color(0xFF1B6EF3) // Seed
val Blue30 = Color(0xFF00458E)
val Blue20 = Color(0xFF002F65)
val Blue10 = Color(0xFF001A40)

// ─── Secondary (Cool Grey-Blue) ─────────────────────────────────
val GreyBlue100 = Color(0xFFE8EBF6)
val GreyBlue90 = Color(0xFFBFC6DC)
val GreyBlue80 = Color(0xFFA3AAC0)
val GreyBlue40 = Color(0xFF575E71)
val GreyBlue30 = Color(0xFF3F4759)
val GreyBlue20 = Color(0xFF293041)
val GreyBlue10 = Color(0xFF141B2C)

// ─── Tertiary (Mauve) ───────────────────────────────────────────
val Mauve100 = Color(0xFFFFD7FA)
val Mauve90 = Color(0xFFDFBBDD)
val Mauve80 = Color(0xFFC4A0C2)
val Mauve40 = Color(0xFF725572)
val Mauve30 = Color(0xFF583E59)
val Mauve20 = Color(0xFF402842)
val Mauve10 = Color(0xFF2A132C)

// ─── Error (Red) ────────────────────────────────────────────────
val Red100 = Color(0xFFFFDAD6)
val Red90 = Color(0xFFFFB4AB)
val Red80 = Color(0xFFFF897D)
val Red40 = Color(0xFFBA1A1A)
val Red30 = Color(0xFF93000A)
val Red20 = Color(0xFF690005)
val Red10 = Color(0xFF410002)

// ─── Neutral / Surface ──────────────────────────────────────────
val Neutral100 = Color(0xFFFCFCFF)
val Neutral95 = Color(0xFFF4F4FA)
val Neutral90 = Color(0xFFE8E8F0)
val Neutral80 = Color(0xFFCCCCD4)
val Neutral70 = Color(0xFFB1B2BB)
val Neutral60 = Color(0xFF9697A1)
val Neutral50 = Color(0xFF7B7C86)
val Neutral40 = Color(0xFF61626C)
val Neutral30 = Color(0xFF494A53)
val Neutral20 = Color(0xFF33343D)
val Neutral10 = Color(0xFF1A1B1F)
val Neutral5 = Color(0xFF0E0F13)

// ─── Extended Colors ────────────────────────────────────────────
val Success = Color(0xFF2E7D32)
val OnSuccess = Color.White
val SuccessContainer = Color(0xFFC8E6C9)
val OnSuccessContainer = Color(0xFF002106)

val Warning = Color(0xFFED6C02)
val OnWarning = Color.White
val WarningContainer = Color(0xFFFFE0B2)
val OnWarningContainer = Color(0xFF3D1700)

val Info = Color(0xFF0288D1)
val OnInfo = Color.White
val InfoContainer = Color(0xFFB3E5FC)
val OnInfoContainer = Color(0xFF001F2E)

// ─── Light Color Scheme ─────────────────────────────────────────
val DoitshLightColorScheme = lightColorScheme(
    // Primary
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue10,

    // Secondary
    secondary = GreyBlue40,
    onSecondary = Color.White,
    secondaryContainer = GreyBlue100,
    onSecondaryContainer = GreyBlue10,

    // Tertiary
    tertiary = Mauve40,
    onTertiary = Color.White,
    tertiaryContainer = Mauve100,
    onTertiaryContainer = Mauve10,

    // Error
    error = Red40,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red10,

    // Background / Surface
    background = Neutral100,
    onBackground = Neutral10,

    surface = Neutral100,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral40,

    // Surface container roles
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F4FA),
    surfaceContainer = Color(0xFFEEEEF4),
    surfaceContainerHigh = Color(0xFFE8E8F0),
    surfaceContainerHighest = Color(0xFFE3E3EB),

    // Surface bright/dim
    surfaceBright = Neutral100,
    surfaceDim = Color(0xFFDADAE2),

    // Outline
    outline = Neutral50,
    outlineVariant = Neutral80,

    // Inverse
    inverseSurface = Neutral20,
    inverseOnSurface = Color(0xFFF0F0F8),
    inversePrimary = Blue90,

    // Utility
    scrim = Color.Black,
)

// ─── Dark Color Scheme ──────────────────────────────────────────
val DoitshDarkColorScheme = darkColorScheme(
    // Primary
    primary = Blue90,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue100,

    // Secondary
    secondary = GreyBlue90,
    onSecondary = GreyBlue20,
    secondaryContainer = GreyBlue30,
    onSecondaryContainer = GreyBlue100,

    // Tertiary
    tertiary = Mauve90,
    onTertiary = Mauve20,
    tertiaryContainer = Mauve30,
    onTertiaryContainer = Mauve100,

    // Error
    error = Red90,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red100,

    // Background / Surface
    background = Neutral10,
    onBackground = Neutral90,

    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,

    // Surface container roles (dark: lighter than surface)
    surfaceContainerLowest = Neutral5,
    surfaceContainerLow = Color(0xFF1E1F25),
    surfaceContainer = Color(0xFF23242A),
    surfaceContainerHigh = Color(0xFF2D2E34),
    surfaceContainerHighest = Color(0xFF38393F),

    // Surface bright/dim
    surfaceBright = Color(0xFF3B3C45),
    surfaceDim = Neutral10,

    // Outline
    outline = Neutral60,
    outlineVariant = Neutral30,

    // Inverse
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Blue40,

    // Utility
    scrim = Color.Black,
)
