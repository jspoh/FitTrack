package com.example.fittrack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CoralPink,
    onPrimary = TextOnPrimary,
    secondary = SkyBlue,
    onSecondary = TextOnPrimary,
    tertiary = MintGreen,
    onTertiary = TextOnPrimary,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF2B2B2B),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = TextMid,
    error = ErrorRed,
    onError = TextOnPrimary,
    outline = Color(0xFF4A4A4A)
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPink,
    onPrimary = TextOnPrimary,
    secondary = SkyBlue,
    onSecondary = TextOnPrimary,
    tertiary = MintGreen,
    onTertiary = TextOnPrimary,
    background = WarmCream,
    onBackground = TextDark,
    surface = SurfaceCard,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFFFF0F4),
    onSurfaceVariant = TextMid,
    error = ErrorRed,
    onError = TextOnPrimary,
    outline = DividerColor
)

@Composable
fun FitTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
