package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val SolarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SolarTheme(
    amoledBlack: Boolean = true,
    accentIntensity: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val primaryBg = if (amoledBlack) SolarDeepBlack else SolarSecondaryBackground
    val surfaceBg = if (amoledBlack) SolarSecondaryBackground else SolarCardBackground
    val primaryAccent = if (accentIntensity >= 0.85f) SolarBurntOrange else SolarPrimaryOrange

    val colorScheme = darkColorScheme(
        primary = primaryAccent,
        onPrimary = Color.White,
        primaryContainer = SolarPrimaryOrange.copy(alpha = 0.25f),
        onPrimaryContainer = SolarWarmAmber,
        secondary = SolarWarmAmber,
        onSecondary = SolarDeepBlack,
        secondaryContainer = SolarCardBackground,
        onSecondaryContainer = SolarWhite,
        tertiary = SolarPrimaryOrange,
        onTertiary = SolarWhite,
        background = primaryBg,
        onBackground = SolarWhite,
        surface = surfaceBg,
        onSurface = SolarWhite,
        surfaceVariant = SolarCardBackground,
        onSurfaceVariant = SolarMutedGrey,
        outline = SolarDivider,
        error = SolarErrorRed,
        onError = SolarWhite
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SolarShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    SolarTheme(content = content)
}
