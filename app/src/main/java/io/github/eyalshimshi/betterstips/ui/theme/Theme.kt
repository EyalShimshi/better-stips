package io.github.eyalshimshi.betterstips.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.eyalshimshi.betterstips.data.AppSettings
import io.github.eyalshimshi.betterstips.data.ThemeMode

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }
val LocalIsDark = staticCompositionLocalOf { false }

private val AppTypography = Typography()

@Composable
fun isDarkTheme(settings: AppSettings): Boolean = when (settings.themeMode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun StipsTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings)
    val context = LocalContext.current
    val base = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        remember(settings.accent, dark) { SchemeGenerator.palettes(settings.accent).toColorScheme(dark) }
    }
    val scheme = if (dark && settings.pureBlack) base.pureBlack() else base
    CompositionLocalProvider(LocalAppSettings provides settings, LocalIsDark provides dark) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}

/** Re-themes a subtree from one color, for example a question in a channel ("content tints the hero"). */
@Composable
fun SeededTheme(seed: Int?, content: @Composable () -> Unit) {
    if (seed == null) {
        content()
        return
    }
    val dark = LocalIsDark.current
    val pureBlack = LocalAppSettings.current.pureBlack
    val scheme = remember(seed, dark, pureBlack) {
        val generated = SchemeGenerator.palettes(seed).toColorScheme(dark)
        if (dark && pureBlack) generated.pureBlack() else generated
    }
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
}

fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0C),
    surfaceContainer = Color(0xFF111114),
    surfaceContainerHigh = Color(0xFF18181B),
    surfaceContainerHighest = Color(0xFF1F1F23),
)

/** Tone table from DESIGN-PHILOSOPHY Appendix A. */
fun SchemeGenerator.Palettes.toColorScheme(dark: Boolean): ColorScheme {
    val p = primary
    val s = secondary
    val t = tertiary
    val n = neutral
    val nv = neutralVariant
    val e = error
    fun c(palette: SchemeGenerator.TonalPalette, tone: Int) = Color(palette.tone(tone))
    return if (!dark) {
        lightColorScheme(
            primary = c(p, 40), onPrimary = c(p, 100), primaryContainer = c(p, 90), onPrimaryContainer = c(p, 10),
            inversePrimary = c(p, 80),
            secondary = c(s, 40), onSecondary = c(s, 100), secondaryContainer = c(s, 90), onSecondaryContainer = c(s, 10),
            tertiary = c(t, 40), onTertiary = c(t, 100), tertiaryContainer = c(t, 90), onTertiaryContainer = c(t, 10),
            background = c(n, 98), onBackground = c(n, 10), surface = c(n, 98), onSurface = c(n, 10),
            surfaceVariant = c(nv, 90), onSurfaceVariant = c(nv, 30), surfaceTint = c(p, 40),
            inverseSurface = c(n, 20), inverseOnSurface = c(n, 95),
            error = c(e, 40), onError = c(e, 100), errorContainer = c(e, 90), onErrorContainer = c(e, 10),
            outline = c(nv, 50), outlineVariant = c(nv, 80), scrim = Color.Black,
            surfaceBright = c(n, 98), surfaceDim = c(n, 87),
            surfaceContainerLowest = c(n, 100), surfaceContainerLow = c(n, 96), surfaceContainer = c(n, 94),
            surfaceContainerHigh = c(n, 92), surfaceContainerHighest = c(n, 90),
            primaryFixed = c(p, 90), primaryFixedDim = c(p, 80), onPrimaryFixed = c(p, 10), onPrimaryFixedVariant = c(p, 30),
            secondaryFixed = c(s, 90), secondaryFixedDim = c(s, 80), onSecondaryFixed = c(s, 10), onSecondaryFixedVariant = c(s, 30),
            tertiaryFixed = c(t, 90), tertiaryFixedDim = c(t, 80), onTertiaryFixed = c(t, 10), onTertiaryFixedVariant = c(t, 30),
        )
    } else {
        darkColorScheme(
            primary = c(p, 80), onPrimary = c(p, 20), primaryContainer = c(p, 30), onPrimaryContainer = c(p, 90),
            inversePrimary = c(p, 40),
            secondary = c(s, 80), onSecondary = c(s, 20), secondaryContainer = c(s, 30), onSecondaryContainer = c(s, 90),
            tertiary = c(t, 80), onTertiary = c(t, 20), tertiaryContainer = c(t, 30), onTertiaryContainer = c(t, 90),
            background = c(n, 6), onBackground = c(n, 90), surface = c(n, 6), onSurface = c(n, 90),
            surfaceVariant = c(nv, 30), onSurfaceVariant = c(nv, 80), surfaceTint = c(p, 80),
            inverseSurface = c(n, 90), inverseOnSurface = c(n, 20),
            error = c(e, 80), onError = c(e, 20), errorContainer = c(e, 30), onErrorContainer = c(e, 90),
            outline = c(nv, 60), outlineVariant = c(nv, 30), scrim = Color.Black,
            surfaceBright = c(n, 24), surfaceDim = c(n, 6),
            surfaceContainerLowest = c(n, 4), surfaceContainerLow = c(n, 10), surfaceContainer = c(n, 12),
            surfaceContainerHigh = c(n, 17), surfaceContainerHighest = c(n, 22),
            primaryFixed = c(p, 90), primaryFixedDim = c(p, 80), onPrimaryFixed = c(p, 10), onPrimaryFixedVariant = c(p, 30),
            secondaryFixed = c(s, 90), secondaryFixedDim = c(s, 80), onSecondaryFixed = c(s, 10), onSecondaryFixedVariant = c(s, 30),
            tertiaryFixed = c(t, 90), tertiaryFixedDim = c(t, 80), onTertiaryFixed = c(t, 10), onTertiaryFixedVariant = c(t, 30),
        )
    }
}
