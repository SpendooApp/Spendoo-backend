package org.spendoo.statistics.util

import java.awt.Color

data class ThemeColors(
    val background: Color,
    val text: Color,
    val gridline: Color,
    val cardBg: Color
)

object ChartTheme {
    val ORANGE_COLOR = Color(234, 88, 12)
    val BLUE_COLOR = Color(23, 159, 221)
    val GREEN_COLOR = Color(0, 204, 122)

    val DONUT_PALETTE = arrayOf(
        Color(234, 88, 12),
        Color(220, 38, 38),
        Color(0, 204, 122),
        Color(23, 159, 221),
        Color(154, 131, 206),
        Color(15, 123, 174)
    )

    val LIGHT = ThemeColors(
        background = Color(255, 255, 255),
        text = Color(129, 147, 177),
        gridline = Color(224, 228, 235),
        cardBg = Color(255, 255, 255)
    )

    val DARK = ThemeColors(
        background = Color(15, 23, 42),
        text = Color(129, 147, 177),
        gridline = Color(51, 65, 85),
        cardBg = Color(30, 41, 59)
    )

    fun getTheme(theme: org.spendoo.statistics.model.Theme): ThemeColors {
        return if (theme == org.spendoo.statistics.model.Theme.DARK) DARK else LIGHT
    }
}
