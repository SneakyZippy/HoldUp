package com.holdup.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ZenSage,
    onPrimary = ZenBackground,
    primaryContainer = ZenSageContainer,
    onPrimaryContainer = ZenSage,
    secondary = ZenLavender,
    onSecondary = ZenBackground,
    secondaryContainer = ZenLavenderContainer,
    onSecondaryContainer = ZenLavender,
    tertiary = ZenCoral,
    background = ZenBackground,
    onBackground = ZenTextPrimary,
    surface = ZenSurface,
    onSurface = ZenTextPrimary,
    surfaceVariant = ZenSurfaceVariant,
    onSurfaceVariant = ZenTextSecondary
)

@Composable
fun HoldUpTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        else -> DarkColorScheme
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
