package org.spendoo.statistics.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import org.spendoo.i18n.I18nService
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.Theme
import java.math.BigDecimal
import javax.imageio.ImageIO

class ChartRendererTest {

    @Test
    fun `renderLineChart returns valid PNG bytes`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "Label"

        val labels = listOf("Jan", "Feb", "Mar", "Apr", "May")
        val budgetData = listOf(
            BigDecimal("1000.00"),
            BigDecimal("1200.00"),
            BigDecimal("1100.00"),
            BigDecimal("1300.00"),
            BigDecimal("1250.00")
        )
        val spentData = listOf(
            BigDecimal("950.00"),
            BigDecimal("1100.00"),
            BigDecimal("1050.00")
        )
        val forecastData = listOf(
            BigDecimal("1050.00"),
            BigDecimal("1150.00")
        )

        val result = ChartRenderer.renderLineChart(
            labels = labels,
            budgetData = budgetData,
            spentData = spentData,
            forecastData = forecastData,
            forecastStartIdx = 2,
            theme = Theme.LIGHT,
            lang = Language.EN,
            i18nService = i18nService
        )

        assertThat(result).isNotEmpty()
        assertThat(result[0]).isEqualTo(0x89.toByte())
        assertThat(result[1]).isEqualTo(0x50.toByte())
        assertThat(result[2]).isEqualTo(0x4E.toByte())
        assertThat(result[3]).isEqualTo(0x47.toByte())

        val image = ImageIO.read(result.inputStream())
        assertThat(image).isNotNull()
        assertThat(image.width).isEqualTo(600)
        assertThat(image.height).isEqualTo(400)
    }

    @Test
    fun `renderBarChart returns valid PNG bytes`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "Label"

        val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
        val budgetData = listOf(
            BigDecimal("1000.00"),
            BigDecimal("1200.00"),
            BigDecimal("1100.00"),
            BigDecimal("1300.00"),
            BigDecimal("1250.00"),
            BigDecimal("1150.00")
        )
        val spentData = listOf(
            BigDecimal("850.00"),
            BigDecimal("1100.00"),
            BigDecimal("950.00"),
            BigDecimal("1400.00"),
            BigDecimal("1200.00"),
            BigDecimal("900.00")
        )

        val result = ChartRenderer.renderBarChart(
            labels = labels,
            budgetData = budgetData,
            spentData = spentData,
            theme = Theme.LIGHT,
            lang = Language.EN,
            i18nService = i18nService
        )

        assertThat(result).isNotEmpty()
        assertThat(result[0]).isEqualTo(0x89.toByte())
        assertThat(result[1]).isEqualTo(0x50.toByte())
        assertThat(result[2]).isEqualTo(0x4E.toByte())
        assertThat(result[3]).isEqualTo(0x47.toByte())

        val image = ImageIO.read(result.inputStream())
        assertThat(image).isNotNull()
        assertThat(image.width).isEqualTo(600)
        assertThat(image.height).isEqualTo(400)
    }

    @Test
    fun `renderDonutChart returns valid PNG bytes`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "Total"

        val categoryNames = listOf("Food", "Shopping", "Transport", "Entertainment", "Other")
        val categoryAmounts = listOf(
            BigDecimal("350.00"),
            BigDecimal("200.00"),
            BigDecimal("150.00"),
            BigDecimal("100.00"),
            BigDecimal("50.00")
        )
        val totalSpent = BigDecimal("850.00")

        val result = ChartRenderer.renderDonutChart(
            totalSpent = totalSpent,
            categoryNames = categoryNames,
            categoryAmounts = categoryAmounts,
            theme = Theme.LIGHT,
            lang = Language.EN,
            i18nService = i18nService
        )

        assertThat(result).isNotEmpty()
        assertThat(result[0]).isEqualTo(0x89.toByte())
        assertThat(result[1]).isEqualTo(0x50.toByte())
        assertThat(result[2]).isEqualTo(0x4E.toByte())
        assertThat(result[3]).isEqualTo(0x47.toByte())

        val image = ImageIO.read(result.inputStream())
        assertThat(image).isNotNull()
        assertThat(image.width).isEqualTo(550)
        assertThat(image.height).isEqualTo(350)
    }

    @Test
    fun `renderLineChart with dark theme returns valid PNG`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "Label"

        val labels = listOf("Mon", "Tue", "Wed")
        val budgetData = listOf(BigDecimal("100.00"), BigDecimal("150.00"), BigDecimal("200.00"))
        val spentData = listOf(BigDecimal("90.00"), BigDecimal("140.00"))
        val forecastData = listOf(BigDecimal("140.00"))

        val result = ChartRenderer.renderLineChart(
            labels = labels,
            budgetData = budgetData,
            spentData = spentData,
            forecastData = forecastData,
            forecastStartIdx = 1,
            theme = Theme.DARK,
            lang = Language.EN,
            i18nService = i18nService
        )

        assertThat(result).isNotEmpty()
        val image = ImageIO.read(result.inputStream())
        assertThat(image).isNotNull()
    }
}
