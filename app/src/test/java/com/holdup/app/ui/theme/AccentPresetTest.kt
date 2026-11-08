package com.holdup.app.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AccentPresetTest {

    @Test
    fun fromKey_returnsCorrectPreset() {
        assertEquals(AccentPreset.SAGE, AccentPreset.fromKey("sage"))
        assertEquals(AccentPreset.IRIS, AccentPreset.fromKey("iris"))
        assertEquals(AccentPreset.GLACIER, AccentPreset.fromKey("glacier"))
        assertEquals(AccentPreset.AMBER, AccentPreset.fromKey("amber"))
        assertEquals(AccentPreset.ROSE, AccentPreset.fromKey("rose"))
    }

    @Test
    fun fromKey_caseInsensitive() {
        assertEquals(AccentPreset.SAGE, AccentPreset.fromKey("SAGE"))
        assertEquals(AccentPreset.IRIS, AccentPreset.fromKey("Iris"))
        assertEquals(AccentPreset.GLACIER, AccentPreset.fromKey("gLaCiEr"))
    }

    @Test
    fun fromKey_backwardsCompatibility_lavenderMapsToIris() {
        assertEquals(AccentPreset.IRIS, AccentPreset.fromKey("lavender"))
        assertEquals(AccentPreset.IRIS, AccentPreset.fromKey("LAVENDER"))
    }

    @Test
    fun fromKey_fallbackToDefaultOnNullOrUnknown() {
        assertEquals(AccentPreset.DEFAULT, AccentPreset.fromKey(null))
        assertEquals(AccentPreset.DEFAULT, AccentPreset.fromKey(""))
        assertEquals(AccentPreset.DEFAULT, AccentPreset.fromKey("non_existent_preset"))
    }

    @Test
    fun allPresets_haveDistinctKeysAndNonEmptyGradients() {
        val keys = AccentPreset.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size, "All preset keys should be unique")

        for (preset in AccentPreset.entries) {
            assertTrue(preset.gradientColors.size >= 2, "Gradient should contain at least 2 colors")
            assertNotEquals(0f, preset.primary.alpha, "Primary color should not be transparent")
        }
    }
}
