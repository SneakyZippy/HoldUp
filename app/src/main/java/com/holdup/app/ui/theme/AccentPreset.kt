package com.holdup.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Curated mindful accent color presets for HoldUp.
 * Provides harmonious color palettes tuned for deep OLED surfaces and serene contrast.
 */
enum class AccentPreset(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val onPrimary: Color = ZenBackground,
    val gradientColors: List<Color>
) {
    SAGE(
        key = "sage",
        displayName = "Zen Sage",
        subtitle = "Restorative emerald",
        primary = Color(0xFF4EE1A0),
        primaryContainer = Color(0xFF132B21),
        onPrimaryContainer = Color(0xFF7AF2BC),
        gradientColors = listOf(Color(0xFF4EE1A0), Color(0xFF38D39F))
    ),
    IRIS(
        key = "iris",
        displayName = "Mindful Iris",
        subtitle = "Serene clarity",
        primary = Color(0xFFA594F9),
        primaryContainer = Color(0xFF241C42),
        onPrimaryContainer = Color(0xFFD2C7FC),
        gradientColors = listOf(Color(0xFFA594F9), Color(0xFF7E60BF))
    ),
    GLACIER(
        key = "glacier",
        displayName = "Glacier Blue",
        subtitle = "Crisp stillness",
        primary = Color(0xFF48CAE4),
        primaryContainer = Color(0xFF112938),
        onPrimaryContainer = Color(0xFFA6E7F4),
        gradientColors = listOf(Color(0xFF48CAE4), Color(0xFF0096C7))
    ),
    AMBER(
        key = "amber",
        displayName = "Golden Sunset",
        subtitle = "Warm grounding",
        primary = Color(0xFFFFB26B),
        primaryContainer = Color(0xFF3D2614),
        onPrimaryContainer = Color(0xFFFFD9B8),
        gradientColors = listOf(Color(0xFFFFB26B), Color(0xFFF4A261))
    ),
    ROSE(
        key = "rose",
        displayName = "Sakura Rose",
        subtitle = "Gentle compassion",
        primary = Color(0xFFF78DA7),
        primaryContainer = Color(0xFF3D1A27),
        onPrimaryContainer = Color(0xFFFFC1D1),
        gradientColors = listOf(Color(0xFFF78DA7), Color(0xFFE56B6F))
    );

    companion object {
        val DEFAULT = SAGE

        fun fromKey(key: String?): AccentPreset {
            if (key == null) return DEFAULT
            // Support backwards compatibility or alternate casing
            return entries.firstOrNull {
                it.key.equals(key, ignoreCase = true) ||
                (key.equals("lavender", ignoreCase = true) && it == IRIS)
            } ?: DEFAULT
        }
    }
}
