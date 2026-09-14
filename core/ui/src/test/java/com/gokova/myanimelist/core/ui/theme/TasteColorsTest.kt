package com.gokova.myanimelist.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class TasteColorsTest {
    @Test
    fun getColorScheme_returnsConsistentSchemeForIndices() {
        val scheme0 = TasteColors.getColorScheme(0, isDark = false)
        val scheme15 = TasteColors.getColorScheme(15, isDark = false)
        assertEquals(scheme0, scheme15)

        val schemeNegative = TasteColors.getColorScheme(-1, isDark = false)
        val scheme14 = TasteColors.getColorScheme(14, isDark = false)
        assertEquals(scheme14, schemeNegative)
    }

    @Test
    fun getColorScheme_all15LightPalettesMeetWcagAaContrast() {
        for (i in 0 until 15) {
            val scheme = TasteColors.getColorScheme(i, isDark = false)
            assertNotNull(scheme)
            val contrast = calculateContrastRatio(scheme.container, scheme.onContainer)
            assertTrue(
                "Light scheme $i contrast $contrast must be >= 4.5",
                contrast >= 4.5,
            )
        }
    }

    @Test
    fun getColorScheme_all15DarkPalettesMeetWcagAaContrast() {
        for (i in 0 until 15) {
            val scheme = TasteColors.getColorScheme(i, isDark = true)
            assertNotNull(scheme)
            val contrast = calculateContrastRatio(scheme.container, scheme.onContainer)
            assertTrue(
                "Dark scheme $i contrast $contrast must be >= 4.5",
                contrast >= 4.5,
            )
        }
    }

    private fun calculateContrastRatio(
        c1: Color,
        c2: Color,
    ): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val brighter = max(l1, l2)
        val darker = min(l1, l2)
        return (brighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        fun linearize(c: Float): Double =
            if (c <= 0.04045f) {
                c / 12.92
            } else {
                ((c + 0.055) / 1.055).pow(2.4)
            }

        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
