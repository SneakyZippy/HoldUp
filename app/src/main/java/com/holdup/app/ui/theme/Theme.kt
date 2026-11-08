package com.holdup.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAccentPreset = staticCompositionLocalOf { AccentPreset.DEFAULT }

/**
 * Builds a mindful dark color scheme dynamically tuned with the active accent preset.
 */
fun buildColorScheme(accent: AccentPreset) = darkColorScheme(
    primary = accent.primary,
    onPrimary = accent.onPrimary,
    primaryContainer = accent.primaryContainer,
    onPrimaryContainer = accent.onPrimaryContainer,
    secondary = ZenLavender,
    onSecondary = ZenBackground,
    secondaryContainer = ZenLavenderContainer,
    onSecondaryContainer = ZenLavender,
    tertiary = ZenCoral,
    onTertiary = ZenBackground,
    tertiaryContainer = ZenCoralContainer,
    onTertiaryContainer = ZenCoral,
    background = ZenBackground,
    onBackground = ZenTextPrimary,
    surface = ZenSurface,
    onSurface = ZenTextPrimary,
    surfaceVariant = ZenSurfaceVariant,
    onSurfaceVariant = ZenTextSecondary,
    surfaceContainerLowest = ZenSurfaceContainerLowest,
    surfaceContainerLow = ZenSurfaceContainerLow,
    surfaceContainer = ZenSurfaceContainer,
    surfaceContainerHigh = ZenSurfaceContainerHigh,
    surfaceContainerHighest = ZenSurfaceContainerHighest,
    outline = ZenBorderStrong,
    outlineVariant = ZenBorder
)

@Composable
fun HoldUpTheme(
    accentPreset: AccentPreset = AccentPreset.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        else -> buildColorScheme(accentPreset)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let { w ->
                @Suppress("DEPRECATION")
                w.statusBarColor = colorScheme.background.toArgb()
                @Suppress("DEPRECATION")
                w.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    CompositionLocalProvider(LocalAccentPreset provides accentPreset) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
