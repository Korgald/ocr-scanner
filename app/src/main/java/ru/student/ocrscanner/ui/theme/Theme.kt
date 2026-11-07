package ru.student.ocrscanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import ru.student.ocrscanner.data.settings.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Pink500,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Ind600,
    onPrimaryContainer = Rose100,
    secondary = Sage300,
    onSecondary = Egg900,
    secondaryContainer = Egg800,
    onSecondaryContainer = Rose100,
    tertiary = Coral400,
    onTertiary = Egg900,
    tertiaryContainer = Coral600,
    onTertiaryContainer = Rose100,
    background = Egg900,
    onBackground = Lilac100,
    surface = Egg900,
    onSurface = Lilac100,
    surfaceVariant = Egg800,
    onSurfaceVariant = Sage300,
    surfaceTint = Pink500,
    outline = Egg600,
    outlineVariant = Egg800,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000),
    inverseSurface = Lilac100,
    inverseOnSurface = Egg900,
    inversePrimary = Pink200
)

private val LightColors = lightColorScheme(
    primary = Ind600,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Lilac200,
    onPrimaryContainer = Egg900,
    secondary = Pink500,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Pink200,
    onSecondaryContainer = Egg900,
    tertiary = Coral600,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Coral200,
    onTertiaryContainer = Color(0xFF40201B),
    background = Mist050,
    onBackground = Egg900,
    surface = Mist050,
    onSurface = Egg900,
    surfaceVariant = Lilac200,
    onSurfaceVariant = Egg700,
    surfaceTint = Ind600,
    outline = Sage300,
    outlineVariant = Rose300,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000),
    inverseSurface = Egg900,
    inverseOnSurface = Lilac100,
    inversePrimary = Pink500
)

/**
 * App theme: eggplant/indigo/pink contest palette.
 * Honors user theme mode and custom font scale.
 */
@Composable
fun OcrScannerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDark) DarkColors else LightColors

    val density = LocalDensity.current
    val scaledDensity = Density(density.density, density.fontScale * fontScale)

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OcrTypography,
            content = content
        )
    }
}