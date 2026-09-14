package com.gokova.myanimelist.core.ui.theme

import androidx.compose.ui.graphics.Color

data class BubbleColorScheme(
    val container: Color,
    val onContainer: Color,
)

object TasteColors {
    val lightBubblePalettes: List<BubbleColorScheme> =
        listOf(
            // 0: Sky Blue
            BubbleColorScheme(Color(0xFFD8E6FF), Color(0xFF001D40)),
            // 1: Lavender Violet
            BubbleColorScheme(Color(0xFFF2DAFF), Color(0xFF251431)),
            // 2: Soft Mint
            BubbleColorScheme(Color(0xFFD4F3DE), Color(0xFF0A3818)),
            // 3: Soft Peach
            BubbleColorScheme(Color(0xFFFFE3C7), Color(0xFF381A00)),
            // 4: Rose Blush
            BubbleColorScheme(Color(0xFFFFD9E2), Color(0xFF3B071D)),
            // 5: Teal Seafoam
            BubbleColorScheme(Color(0xFFCCEBEB), Color(0xFF003738)),
            // 6: Honey Yellow
            BubbleColorScheme(Color(0xFFFFF0B3), Color(0xFF362B00)),
            // 7: Slate Indigo
            BubbleColorScheme(Color(0xFFDFE0FF), Color(0xFF00105C)),
            // 8: Coral Salmon
            BubbleColorScheme(Color(0xFFFFDAD4), Color(0xFF3E0400)),
            // 9: Emerald Sage
            BubbleColorScheme(Color(0xFFCEEBD4), Color(0xFF00391A)),
            // 10: Lilac Mauve
            BubbleColorScheme(Color(0xFFEADBFF), Color(0xFF24005A)),
            // 11: Ice Cyan
            BubbleColorScheme(Color(0xFFC4EBF8), Color(0xFF003544)),
            // 12: Warm Ochre
            BubbleColorScheme(Color(0xFFF6E1C4), Color(0xFF2E1B02)),
            // 13: Wine Plum
            BubbleColorScheme(Color(0xFFFFD6F6), Color(0xFF36003B)),
            // 14: Moss Olive
            BubbleColorScheme(Color(0xFFE0E7C0), Color(0xFF1B2302)),
        )

    val darkBubblePalettes: List<BubbleColorScheme> = lightBubblePalettes

    fun getColorScheme(
        index: Int,
        isDark: Boolean,
    ): BubbleColorScheme {
        val palette = if (isDark) darkBubblePalettes else lightBubblePalettes
        val safeIndex = (index % palette.size + palette.size) % palette.size
        return palette[safeIndex]
    }
}
