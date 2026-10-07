package com.hpg.goaltracker.ui.theme

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
import com.hpg.goaltracker.data.AppSettings

private val DarkColorScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletDark,
    onPrimaryContainer = Color.White,
    secondary = Teal,
    onSecondary = Color(0xFF00201B),
    secondaryContainer = Color(0xFF075C52),
    onSecondaryContainer = TealContainer,
    tertiary = Gold,
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF7A5A00),
    onTertiaryContainer = GoldContainer,
    background = DarkBackground,
    onBackground = Color(0xFFEAE7F2),
    surface = DarkSurface,
    onSurface = Color(0xFFEAE7F2),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC9C4D6),
    outline = Color(0xFF4A4654),
)

private val LightColorScheme = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletContainer,
    onPrimaryContainer = VioletDark,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Gold,
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = Color(0xFF5A3F00),
    background = LightBackground,
    onBackground = Color(0xFF1B1A22),
    surface = LightSurface,
    onSurface = Color(0xFF1B1A22),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4A475A),
    outline = Color(0xFFCAC5DA),
)

@Composable
fun GoalTrackerTheme(
    themeMode: Int = AppSettings.THEME_SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
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
