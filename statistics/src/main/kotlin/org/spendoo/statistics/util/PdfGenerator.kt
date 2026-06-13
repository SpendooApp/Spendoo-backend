package org.spendoo.statistics.util

import com.openhtmltopdf.bidi.support.ICUBidiReorderer
import com.openhtmltopdf.bidi.support.ICUBidiSplitter
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.TextDirection
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.spendoo.i18n.I18nService
import org.spendoo.statistics.api.dto.response.StatisticsResponse
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.StatsPeriod
import org.spendoo.statistics.model.Theme
import org.spendoo.statistics.model.TrendDirection
import org.spendoo.transactions.entity.CategoryIcon
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PdfGenerator {

    fun generatePdf(
        stats: StatisticsResponse,
        period: StatsPeriod,
        theme: Theme,
        lang: Language,
        i18nService: I18nService
    ): ByteArray {
        val themeClass = if (theme == Theme.DARK) "dark-theme" else ""
        val directionClass = if (lang == Language.AR) "rtl" else ""
        val dirAttr = if (lang == Language.AR) "rtl" else "ltr"
        
        val locale = lang.locale
        val reportTitle = i18nService.getMessage("report_title", locale)
        val budgetLabel = i18nService.getMessage("budget", locale)
        val spentLabel = i18nService.getMessage("spent", locale)
        val forecastLabel = i18nService.getMessage("forecast", locale)
        val last6Label = i18nService.getMessage("last_6_periods", locale)
        val expensesLabel = i18nService.getMessage("expenses", locale)
        val topCategoriesLabel = i18nService.getMessage("top_categories", locale)
        val totalLabel = i18nService.getMessage("total", locale)
        
        val periodKey = "period.${period.name.lowercase()}"
        val translatedPeriod = i18nService.getMessage(periodKey, locale)
        val generatedAtLabel = i18nService.getMessage("generated_at", locale)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val generatedTime = LocalDateTime.now().format(formatter)

        val categoriesHtml = StringBuilder()
        for (cat in stats.topCategories) {
            val icon = cat.categoryIcon
            val iconChar = getCategoryIconChar(icon, cat.categoryName)
            val iconBg = getCategoryIconBgColor(icon)
            val iconText = getCategoryIconTextColor(icon)

            val changeClass = when (cat.trend) {
                TrendDirection.UP -> "trend-up"
                TrendDirection.DOWN -> "trend-down"
                else -> "trend-flat"
            }

            val trendArrow = when (cat.trend) {
                TrendDirection.UP -> "↑"
                TrendDirection.DOWN -> "↓"
                else -> "→"
            }

            val amountStr = String.format("$%,.2f", cat.amount.toDouble())
            val pctStr = String.format("%.1f%%", cat.percentageChange.toDouble())

            categoriesHtml.append("""
                <div class="category-item">
                    <div class="category-left">
                        <div class="category-icon-bg" style="background: $iconBg; color: $iconText;">$iconChar</div>
                        <span class="category-name">${cat.categoryName}</span>
                    </div>
                    <div class="category-right">
                        <span class="category-amount">$amountStr</span>
                        <span class="category-change $changeClass">$pctStr $trendArrow</span>
                    </div>
                    <div class="clear"></div>
                </div>
            """.trimIndent())
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html dir="$dirAttr">
            <head>
                <meta charset="utf-8"/>
                <style>
                    @page { margin: 0; }
                    @page page1 { size: 800px 800px; }
                    @page page2 { size: 800px 750px; }
                    @page page3 { size: 800px 1200px; }
                    
                    body {
                        font-family: 'Arial', 'DejaVu Sans', sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #F8FAFC;
                        color: #1E293B;
                        -webkit-print-color-adjust: exact;
                        width: 800px;
                    }
                    .dark-theme {
                        background-color: #0F172A;
                        color: #F1F5F9;
                    }
                    .page1 { page: page1; page-break-after: always; padding: 20px; box-sizing: border-box; }
                    .page2 { page: page2; page-break-after: always; padding: 20px; box-sizing: border-box; }
                    .page3 { page: page3; padding: 20px; box-sizing: border-box; }
                    .header {
                        margin-bottom: 20px;
                        border-bottom: 2px solid #E0E4EB;
                        padding-bottom: 15px;
                    }
                    .dark-theme .header {
                        border-bottom: 2px solid #334155;
                    }
                    .header h1 {
                        font-size: 26px;
                        margin: 0;
                        color: #1E293B;
                        font-weight: 700;
                    }
                    .dark-theme .header h1 {
                        color: #F1F5F9;
                    }
                    .header p {
                        font-size: 14px;
                        margin: 5px 0 0 0;
                        color: #8193B1;
                    }
                    .card {
                        background: #FFFFFF;
                        border: 1px solid #E0E4EB;
                        border-radius: 24px;
                        padding: 28px;
                        margin-top: 15px;
                    }
                    .dark-theme .card {
                        background: #1E293B;
                        border: 1px solid #334155;
                    }
                    .card-title {
                        font-size: 20px;
                        font-weight: 700;
                        margin-bottom: 25px;
                        color: #4E607E;
                    }
                    .dark-theme .card-title {
                        color: #A1AEC4;
                    }
                    .chart-container {
                        text-align: center;
                        margin: 20px 0;
                    }
                    .chart-img {
                        max-width: 100%;
                        height: auto;
                        border-radius: 12px;
                    }
                    
                    /* Category list styles */
                    .category-list {
                        margin-top: 20px;
                    }
                    .category-item {
                        border: 1px solid #E0E4EB;
                        border-radius: 16px;
                        padding: 12px 18px;
                        margin-bottom: 10px;
                        background: #FFFFFF;
                    }
                    .dark-theme .category-item {
                        border: 1px solid #334155;
                        background: #1E293B;
                    }
                    .category-left {
                        float: left;
                        width: 50%;
                    }
                    .category-right {
                        float: right;
                        width: 50%;
                        text-align: right;
                    }
                    .category-icon-bg {
                        display: inline-block;
                        width: 38px;
                        height: 38px;
                        border-radius: 12px;
                        text-align: center;
                        line-height: 38px;
                        font-weight: 700;
                        margin-right: 12px;
                        font-size: 16px;
                    }
                    .category-name {
                        font-size: 15px;
                        font-weight: 500;
                        color: #8193B1;
                        display: inline-block;
                        vertical-align: middle;
                    }
                    .category-amount {
                        font-size: 16px;
                        font-weight: 700;
                        color: #4E607E;
                        display: inline-block;
                        vertical-align: middle;
                        margin-right: 20px;
                    }
                    .dark-theme .category-amount {
                        color: #F1F5F9;
                    }
                    .category-change {
                        font-size: 14px;
                        font-weight: 600;
                        display: inline-block;
                        vertical-align: middle;
                    }
                    .trend-up {
                        color: #DC2626;
                    }
                    .trend-down {
                        color: #00CC7A;
                    }
                    .trend-flat {
                        color: #8193B1;
                    }
                    .clear {
                        clear: both;
                    }

                    /* RTL specific layout overrides */
                    .rtl {
                        direction: rtl;
                    }
                    .rtl .category-left {
                        float: right;
                        text-align: right;
                    }
                    .rtl .category-right {
                        float: left;
                        text-align: left;
                    }
                    .rtl .category-icon-bg {
                        margin-right: 0;
                        margin-left: 12px;
                    }
                    .rtl .category-amount {
                        margin-right: 0;
                        margin-left: 20px;
                    }
                </style>
            </head>
            <body class="$themeClass $directionClass">
                
                <div class="page1">
                    <div class="header">
                        <h1>$reportTitle</h1>
                        <p>$translatedPeriod - $generatedAtLabel $generatedTime</p>
                    </div>
                    <div class="card">
                        <div class="card-title">${budgetLabel} &amp; ${spentLabel}</div>
                        <div class="chart-container">
                            <img class="chart-img" src="${stats.lineChart.image}" alt="Line Chart"/>
                        </div>
                    </div>
                </div>

                <div class="page2">
                    <div class="header">
                        <h1>$reportTitle</h1>
                        <p>$translatedPeriod - $generatedAtLabel $generatedTime</p>
                    </div>
                    <div class="card">
                        <div class="card-title">$last6Label</div>
                        <div class="chart-container">
                            <img class="chart-img" src="${stats.barChart.image}" alt="Bar Chart"/>
                        </div>
                    </div>
                </div>

                <div class="page3">
                    <div class="header">
                        <h1>$reportTitle</h1>
                        <p>$translatedPeriod - $generatedAtLabel $generatedTime</p>
                    </div>
                    <div class="card">
                        <div class="card-title">$expensesLabel</div>
                        <div class="chart-container">
                            <img class="chart-img" src="${stats.donutChart.image}" alt="Donut Chart"/>
                        </div>
                        <div class="card-title" style="margin-top: 30px; margin-bottom: 15px;">$topCategoriesLabel</div>
                        <div class="category-list">
                            $categoriesHtml
                        </div>
                    </div>
                </div>

            </body>
            </html>
        """.trimIndent()

        val os = ByteArrayOutputStream()
        val builder = PdfRendererBuilder()
        builder.useFastMode()
        builder.useUnicodeBidiSplitter(ICUBidiSplitter.ICUBidiSplitterFactory())
        builder.useUnicodeBidiReorderer(ICUBidiReorderer())
        if (lang == Language.AR) {
            builder.defaultTextDirection(TextDirection.RTL)
        }
        
        val fontFile = File("C:/Windows/Fonts/arial.ttf")
        if (fontFile.exists()) {
            builder.useFont(fontFile, "Arial")
        }
        val boldFontFile = File("C:/Windows/Fonts/arialbd.ttf")
        if (boldFontFile.exists()) {
            builder.useFont(boldFontFile, "Arial", 700, com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL, true)
        }

        builder.withHtmlContent(htmlContent, null)
        builder.toStream(os)
        builder.run()
        return os.toByteArray()
    }

    private fun getCategoryIconChar(icon: CategoryIcon, categoryName: String): String {
        return when (icon) {
            CategoryIcon.FOOD -> "F"
            CategoryIcon.SHOPPING -> "S"
            CategoryIcon.TRAVEL -> "T"
            CategoryIcon.CAR -> "C"
            CategoryIcon.HEALTHCARE -> "H"
            CategoryIcon.TRANSPORT -> "P"
            CategoryIcon.ENTERTAINMENT -> "E"
            CategoryIcon.EDUCATION -> "D"
            CategoryIcon.MOBILE -> "M"
            CategoryIcon.FINANCE -> "F"
            CategoryIcon.COFFEE -> "C"
            CategoryIcon.GIFTS -> "G"
            CategoryIcon.PETS -> "P"
            CategoryIcon.FITNESS -> "F"
            CategoryIcon.UTILITIES -> "U"
            CategoryIcon.WIFI -> "W"
            else -> {
                if (categoryName.isNotEmpty()) {
                    categoryName.take(1).uppercase()
                } else {
                    "🏷"
                }
            }
        }
    }

    private fun getCategoryIconBgColor(icon: CategoryIcon): String {
        return when (icon) {
            CategoryIcon.FOOD -> "#FFF1F2"
            CategoryIcon.SHOPPING -> "#FFF7ED"
            CategoryIcon.TRAVEL -> "#F5F3FF"
            CategoryIcon.CAR -> "#ECFDF5"
            CategoryIcon.HEALTHCARE -> "#ECFEFF"
            CategoryIcon.TRANSPORT -> "#F0F9FF"
            CategoryIcon.ENTERTAINMENT -> "#FFF1F2"
            else -> "#F8FAFC"
        }
    }

    private fun getCategoryIconTextColor(icon: CategoryIcon): String {
        return when (icon) {
            CategoryIcon.FOOD -> "#DC2626"
            CategoryIcon.SHOPPING -> "#EA5A0C"
            CategoryIcon.TRAVEL -> "#8B5CF6"
            CategoryIcon.CAR -> "#10B981"
            CategoryIcon.HEALTHCARE -> "#06B6D4"
            CategoryIcon.TRANSPORT -> "#179FDD"
            CategoryIcon.ENTERTAINMENT -> "#DC2626"
            else -> "#64748B"
        }
    }
}
