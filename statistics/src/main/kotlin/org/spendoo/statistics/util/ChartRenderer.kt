package org.spendoo.statistics.util

import org.spendoo.i18n.I18nService
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.Theme
import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DecimalFormat
import javax.imageio.ImageIO

object ChartRenderer {

    init {
        System.setProperty("java.awt.headless", "true")
    }

    private fun imageToByteArray(image: BufferedImage): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }

    private fun enableAntiAliasing(g2: Graphics2D) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    }

    fun renderLineChart(
        labels: List<String>,
        budgetData: List<BigDecimal>,
        spentData: List<BigDecimal>,
        forecastData: List<BigDecimal>,
        forecastStartIdx: Int,
        theme: Theme,
        lang: Language,
        i18nService: I18nService
    ): ByteArray {
        val width = 600
        val height = 400
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()
        enableAntiAliasing(g2)

        val themeColors = ChartTheme.getTheme(theme)

        g2.color = themeColors.background
        g2.fillRect(0, 0, width, height)
        
        if (labels.isEmpty()) {
            g2.dispose()
            return imageToByteArray(image)
        }

        val paddingLeft = 70
        val paddingRight = 40
        val paddingTop = 40
        val paddingBottom = 70
        val plotW = width - paddingLeft - paddingRight
        val plotH = height - paddingTop - paddingBottom

        val allValues = (budgetData + spentData + forecastData)
        var maxVal = allValues.maxOfOrNull { it.toDouble() } ?: 1000.0
        if (maxVal <= 0) maxVal = 1000.0
        maxVal = Math.ceil(maxVal / 1000.0) * 1000.0

        val divisions = 5
        val df = DecimalFormat("#,##0")
        for (i in 0..divisions) {
            val ratio = i.toDouble() / divisions
            val y = paddingTop + plotH - (ratio * plotH).toInt()
            val valAtTick = ratio * maxVal

            g2.color = themeColors.gridline
            g2.stroke = BasicStroke(1f)
            g2.drawLine(paddingLeft, y, width - paddingRight, y)

            g2.color = themeColors.text
            g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
            var labelText = df.format(valAtTick)
            if (valAtTick >= 1000) {
                labelText = "${df.format(valAtTick / 1000)}k"
            }
            g2.drawString(labelText, 15, y + 4)
        }

        val n = labels.size
        val colW = if (n > 1) plotW / (n - 1) else plotW

        g2.color = themeColors.text
        g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        for (i in 0 until n) {
            val x = paddingLeft + i * colW
            val label = labels[i]
            val fm = g2.fontMetrics
            val labelW = fm.stringWidth(label)
            g2.drawString(label, x - labelW / 2, height - paddingBottom + 25)
        }

        drawDataLine(g2, budgetData, 0, paddingLeft, paddingTop, plotH, colW, maxVal, ChartTheme.ORANGE_COLOR, false)
        drawDataLine(g2, spentData, 0, paddingLeft, paddingTop, plotH, colW, maxVal, ChartTheme.BLUE_COLOR, false)
        drawDataLine(g2, forecastData, forecastStartIdx, paddingLeft, paddingTop, plotH, colW, maxVal, ChartTheme.BLUE_COLOR, true)

        val legendY = height - 20
        g2.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        val fm = g2.fontMetrics

        val budgetLabel = i18nService.getMessage("budget", lang.locale)
        val spentLabel = i18nService.getMessage("spent", lang.locale)
        val forecastLabel = i18nService.getMessage("forecast", lang.locale)

        val labelsList = listOf(
            Pair(budgetLabel, ChartTheme.ORANGE_COLOR),
            Pair(spentLabel, ChartTheme.BLUE_COLOR),
            Pair(forecastLabel, ChartTheme.BLUE_COLOR)
        )

        var currentX = paddingLeft + 50
        for ((text, color) in labelsList) {
            g2.color = color
            if (text == forecastLabel) {
                g2.stroke = BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(4f, 4f), 0f)
                g2.drawLine(currentX, legendY - 5, currentX + 20, legendY - 5)
            } else {
                g2.stroke = BasicStroke(2f)
                g2.drawLine(currentX, legendY - 5, currentX + 20, legendY - 5)
                g2.fillOval(currentX + 7, legendY - 8, 6, 6)
            }

            g2.color = themeColors.text
            g2.drawString(text, currentX + 25, legendY)
            currentX += 30 + fm.stringWidth(text) + 40
        }

        g2.dispose()
        return imageToByteArray(image)
    }

    private fun drawDataLine(
        g2: Graphics2D,
        data: List<BigDecimal>,
        startIndex: Int,
        paddingLeft: Int,
        paddingTop: Int,
        plotH: Int,
        colW: Int,
        maxVal: Double,
        color: Color,
        isDashed: Boolean
    ) {
        val points = mutableListOf<Point>()
        for (i in data.indices) {
            val value = data[i]
            val x = paddingLeft + (startIndex + i) * colW
            val y = paddingTop + plotH - ((value.toDouble() / maxVal) * plotH).toInt()
            points.add(Point(x, y))
        }

        if (points.size < 2) {
            if (points.size == 1 && !isDashed) {
                val p = points[0]
                g2.stroke = BasicStroke(1f)
                g2.color = Color.WHITE
                g2.fillOval(p.x - 4, p.y - 4, 8, 8)
                g2.color = color
                g2.drawOval(p.x - 4, p.y - 4, 8, 8)
            }
            return
        }

        g2.color = color
        if (isDashed) {
            g2.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, floatArrayOf(8f, 5f), 0f)
        } else {
            g2.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        }

        val path = Path2D.Float()
        path.moveTo(points[0].x.toFloat(), points[0].y.toFloat())
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val ctrl1X = p1.x + (p2.x - p1.x) / 2.5f
            val ctrl1Y = p1.y.toFloat()
            val ctrl2X = p1.x + (p2.x - p1.x) * (1.5f / 2.5f)
            val ctrl2Y = p2.y.toFloat()
            path.curveTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, p2.x.toFloat(), p2.y.toFloat())
        }

        if (!isDashed) {
            val fillPath = path.clone() as Path2D.Float
            fillPath.lineTo(points.last().x.toFloat(), paddingTop.toFloat() + plotH)
            fillPath.lineTo(points.first().x.toFloat(), paddingTop.toFloat() + plotH)
            fillPath.closePath()

            val gradient = GradientPaint(
                0f, paddingTop.toFloat(), Color(color.red, color.green, color.blue, 60),
                0f, paddingTop.toFloat() + plotH, Color(color.red, color.green, color.blue, 0)
            )
            g2.paint = gradient
            g2.fill(fillPath)
        }

        g2.color = color
        g2.draw(path)

        if (!isDashed) {
            g2.stroke = BasicStroke(1f)
            for (p in points) {
                g2.color = Color.WHITE
                g2.fillOval(p.x - 4, p.y - 4, 8, 8)
                g2.color = color
                g2.drawOval(p.x - 4, p.y - 4, 8, 8)
            }
        }
    }

    fun renderBarChart(
        labels: List<String>,
        budgetData: List<BigDecimal>,
        spentData: List<BigDecimal>,
        theme: Theme,
        lang: Language,
        i18nService: I18nService
    ): ByteArray {
        val width = 600
        val height = 400
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()
        enableAntiAliasing(g2)

        val themeColors = ChartTheme.getTheme(theme)

        g2.color = themeColors.background
        g2.fillRect(0, 0, width, height)
        
        if (labels.isEmpty()) {
            g2.dispose()
            return imageToByteArray(image)
        }

        val paddingLeft = 70
        val paddingRight = 40
        val paddingTop = 40
        val paddingBottom = 70
        val plotW = width - paddingLeft - paddingRight
        val plotH = height - paddingTop - paddingBottom

        val allValues = spentData + budgetData
        var maxVal = allValues.maxOfOrNull { it.toDouble() } ?: 1000.0
        if (maxVal <= 0) maxVal = 1000.0
        maxVal = Math.ceil(maxVal / 500.0) * 500.0

        val divisions = 5
        val df = DecimalFormat("#,##0")
        for (i in 0..divisions) {
            val ratio = i.toDouble() / divisions
            val y = paddingTop + plotH - (ratio * plotH).toInt()
            val valAtTick = ratio * maxVal

            g2.color = themeColors.gridline
            g2.drawLine(paddingLeft, y, width - paddingRight, y)

            g2.color = themeColors.text
            g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
            var labelText = df.format(valAtTick)
            if (valAtTick >= 1000) {
                labelText = "${df.format(valAtTick / 1000)}k"
            }
            g2.drawString(labelText, 15, y + 4)
        }

        val numBars = labels.size
        val barGroupW = plotW / numBars
        val barW = 24

        g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        val fm = g2.fontMetrics

        for (i in 0 until numBars) {
            val label = labels[i]
            val spent = spentData[i]
            val budget = budgetData[i]

            val color = when {
                budget.compareTo(BigDecimal.ZERO) == 0 -> {
                    if (spent.compareTo(BigDecimal.ZERO) == 0) ChartTheme.GREEN_COLOR else ChartTheme.BLUE_COLOR
                }
                else -> {
                    val ratio = spent.divide(budget, 4, java.math.RoundingMode.HALF_UP)
                    when {
                        ratio < BigDecimal("0.9") -> ChartTheme.GREEN_COLOR
                        ratio <= BigDecimal("1.0") -> ChartTheme.ORANGE_COLOR
                        else -> ChartTheme.BLUE_COLOR
                    }
                }
            }

            val barH = ((spent.toDouble() / maxVal) * plotH).toInt().coerceAtLeast(4)
            val groupStartX = paddingLeft + i * barGroupW
            val barX = groupStartX + (barGroupW - barW) / 2
            val barY = paddingTop + plotH - barH

            g2.color = color
            g2.fillRoundRect(barX, barY, barW, barH, 8, 8)

            g2.color = themeColors.text
            val labelW = fm.stringWidth(label)
            g2.drawString(label, groupStartX + (barGroupW - labelW) / 2, height - paddingBottom + 25)
        }

        val legendY = height - 20
        g2.font = Font(Font.SANS_SERIF, Font.BOLD, 12)
        val withinLabel = i18nService.getMessage("within", lang.locale)
        val riskLabel = i18nService.getMessage("risk", lang.locale)
        val overspendingLabel = i18nService.getMessage("overspending", lang.locale)

        val legendLabels = listOf(
            Pair(withinLabel, ChartTheme.GREEN_COLOR),
            Pair(riskLabel, ChartTheme.ORANGE_COLOR),
            Pair(overspendingLabel, ChartTheme.BLUE_COLOR)
        )

        val legendFm = g2.fontMetrics
        var currentX = paddingLeft + 30
        for ((text, color) in legendLabels) {
            g2.color = color
            g2.fillOval(currentX, legendY - 10, 10, 10)

            g2.color = themeColors.text
            g2.drawString(text, currentX + 18, legendY - 1)
            currentX += 18 + legendFm.stringWidth(text) + 40
        }

        g2.dispose()
        return imageToByteArray(image)
    }

    fun renderDonutChart(
        totalSpent: BigDecimal,
        categoryNames: List<String>,
        categoryAmounts: List<BigDecimal>,
        theme: Theme,
        lang: Language,
        i18nService: I18nService
    ): ByteArray {
        val width = 550
        val height = 350
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()
        enableAntiAliasing(g2)

        val themeColors = ChartTheme.getTheme(theme)

        g2.color = themeColors.background
        g2.fillRect(0, 0, width, height)
        
        if (categoryNames.isEmpty()) {
            g2.dispose()
            return imageToByteArray(image)
        }

        val donutSize = 200
        val donutX = 50
        val donutY = (height - donutSize) / 2

        val totalAmount = categoryAmounts.fold(BigDecimal.ZERO) { acc, amt -> acc + amt }
        val totalDouble = totalAmount.toDouble()

        var currentAngle = 90.0

        for (i in categoryNames.indices) {
            val amount = categoryAmounts[i].toDouble()
            val angleSize = if (totalDouble > 0) (amount / totalDouble) * 360.0 else 0.0
            val color = ChartTheme.DONUT_PALETTE[i % ChartTheme.DONUT_PALETTE.size]

            g2.color = color
            g2.fillArc(donutX, donutY, donutSize, donutSize, currentAngle.toInt(), -Math.ceil(angleSize).toInt())
            currentAngle -= angleSize
        }

        val centerSize = 130
        val centerX = donutX + (donutSize - centerSize) / 2
        val centerY = donutY + (donutSize - centerSize) / 2
        g2.color = themeColors.background
        g2.fill(Ellipse2D.Double(centerX.toDouble(), centerY.toDouble(), centerSize.toDouble(), centerSize.toDouble()))

        val df = DecimalFormat("$#,##0.00")
        val totalText = df.format(totalSpent)
        val subText = i18nService.getMessage("total", lang.locale)

        g2.color = themeColors.text
        g2.font = Font(Font.SANS_SERIF, Font.BOLD, 18)
        var textFm = g2.fontMetrics
        var textW = textFm.stringWidth(totalText)
        g2.drawString(totalText, donutX + donutSize / 2 - textW / 2, donutY + donutSize / 2 + 5)

        g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        textFm = g2.fontMetrics
        textW = textFm.stringWidth(subText)
        g2.drawString(subText, donutX + donutSize / 2 - textW / 2, donutY + donutSize / 2 + 25)

        val legendStartX = donutX + donutSize + 40
        val legendStartY = donutY + 30
        val itemHeight = 30

        g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)

        for (i in categoryNames.indices) {
            val name = categoryNames[i]
            val amount = categoryAmounts[i]
            val pct = if (totalDouble > 0) (amount.toDouble() / totalDouble * 100.0) else 0.0
            val color = ChartTheme.DONUT_PALETTE[i % ChartTheme.DONUT_PALETTE.size]

            val y = legendStartY + i * itemHeight

            g2.color = color
            g2.fillRoundRect(legendStartX, y, 12, 12, 4, 4)

            g2.color = themeColors.text
            val displayPct = String.format("%.1f%%", pct)
            val legendText = "$name ($displayPct)"
            g2.drawString(legendText, legendStartX + 22, y + 11)
        }

        g2.dispose()
        return imageToByteArray(image)
    }
}
