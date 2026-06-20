package org.spendoo.statistics.util

import com.openhtmltopdf.bidi.support.ICUBidiReorderer
import com.openhtmltopdf.bidi.support.ICUBidiSplitter
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.TextDirection
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.spendoo.i18n.I18nService
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.ReportDataType
import org.spendoo.statistics.model.Theme
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.repository.TransactionViewRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object PdfGenerator {

    fun generateDetailedPdf(
        userId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        reportDataType: ReportDataType,
        theme: Theme,
        lang: Language,
        i18nService: I18nService,
        transactionRepository: TransactionRepository,
        transactionViewRepository: TransactionViewRepository,
    ): ByteArray {
        val themeClass = if (theme == Theme.DARK) "dark-theme" else ""
        val directionClass = if (lang == Language.AR) "rtl" else ""
        val dirAttr = if (lang == Language.AR) "rtl" else "ltr"
        val locale = lang.locale

        val pageBgColor = if (theme == Theme.DARK) "#0F172A" else "#F8FAFC"
        val pageColor = if (theme == Theme.DARK) "#F1F5F9" else "#1E293B"
        val fontFamily = if (lang == Language.AR) "Amiri" else "Roboto"

        val formatter = DateTimeFormatter.ofPattern("MMM dd yyyy", locale)
        val periodStr = "${startDate.format(formatter)} - ${endDate.format(formatter)}"
        
        val income = transactionRepository.sumIncomeByUserIdAndDateRange(userId, startDate, endDate)
        val expenses = transactionRepository.sumExpensesByUserIdAndDateRange(userId, startDate, endDate).negate()
        val saved = income.subtract(expenses)

        val detailedReportTitle = i18nService.getMessage("detailed_report_title", locale, "Detailed Report")
        val totalEarningsLabel = i18nService.getMessage("total_earnings", locale, "Total earnings")
        val totalSpendingLabel = i18nService.getMessage("total_spending", locale, "Total spending")
        val totalSavedLabel = i18nService.getMessage("total_saved", locale, "Total saved")
        val reportPeriodLabel = i18nService.getMessage("report_period", locale, "Report Period")
        
        val typeHeader = i18nService.getMessage("type", locale, "Type")
        val titleHeader = i18nService.getMessage("title", locale, "Title")
        val amountHeader = i18nService.getMessage("amount", locale, "Amount")
        val categoryHeader = i18nService.getMessage("category", locale, "Category")
        val dateHeader = i18nService.getMessage("date", locale, "Date")
        val noteHeader = i18nService.getMessage("note", locale, "Note")

        val headerRowHtml = if (lang == Language.AR) {
            """
                <tr>
                    <th>$noteHeader</th>
                    <th>$dateHeader</th>
                    <th>$categoryHeader</th>
                    <th>$amountHeader</th>
                    <th>$titleHeader</th>
                    <th>$typeHeader</th>
                </tr>
            """.trimIndent()
        } else {
            """
                <tr>
                    <th>$typeHeader</th>
                    <th>$titleHeader</th>
                    <th>$amountHeader</th>
                    <th>$categoryHeader</th>
                    <th>$dateHeader</th>
                    <th>$noteHeader</th>
                </tr>
            """.trimIndent()
        }

        val htmlContent = StringBuilder()
        htmlContent.append("""
            <!DOCTYPE html>
            <html dir="$dirAttr">
            <head>
                <meta charset="utf-8"/>
                <style>
                    @page { 
                        margin: 30px; 
                        size: A4 portrait; 
                        background-color: $pageBgColor;
                    }
                    html {
                        background-color: $pageBgColor;
                    }
                    body {
                        font-family: '$fontFamily', 'DejaVu Sans', sans-serif;
                        font-size: 12px;
                        color: $pageColor;
                        background-color: $pageBgColor;
                    }
                    body.rtl, table.rtl {
                        direction: rtl;
                    }
                    .dark-theme {
                        background-color: #0F172A;
                        color: #F1F5F9;
                    }
                    .header { margin-bottom: 20px; }
                    .header h1 { font-size: 24px; margin: 0; }
                    .summary { margin-top: 15px; font-size: 14px; }
                    .summary div { margin-bottom: 5px; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 20px; background: #FFF; border-radius: 8px; overflow: hidden; }
                    .dark-theme table { background: #1E293B; }
                    th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #E0E4EB; }
                    .dark-theme th, .dark-theme td { border-bottom: 1px solid #334155; }
                    th { background-color: #F1F5F9; font-weight: 600; color: #4E607E; }
                    .dark-theme th { background-color: #334155; color: #A1AEC4; }
                    .amount-in { color: #10B981; font-weight: bold; }
                    .amount-out { color: #EF4444; font-weight: bold; }
                    .rtl th, .rtl td { text-align: right; }
                </style>
            </head>
            <body class="$themeClass $directionClass">
                <div class="header">
                    <h1>$detailedReportTitle</h1>
                    <div class="summary">
                        <div>$totalEarningsLabel: $%,.2f</div>
                        <div>$totalSpendingLabel: $%,.2f</div>
                        <div>$totalSavedLabel: $%,.2f</div>
                        <div style="margin-top: 15px; font-weight: bold;">$reportPeriodLabel</div>
                        <div>$periodStr</div>
                    </div>
                </div>
                <table class="$directionClass">
                    <thead>
                        $headerRowHtml
                    </thead>
                    <tbody>
        """.trimIndent().format(income.toDouble(), expenses.toDouble(), saved.toDouble()))

        var page = 0
        val size = 500
        var hasMore = true

        while (hasMore) {
            val pageRequest = PageRequest.of(page, size, Sort.by("transactionDate").descending())
            val transactions = transactionViewRepository.findAllByUserIdAndTransactionDateBetween(userId, startDate, endDate, pageRequest)
            
            for (t in transactions.content) {
                if (reportDataType == ReportDataType.EXPENSES && t.amount >= BigDecimal.ZERO) continue
                if (reportDataType == ReportDataType.INCOME && t.amount < BigDecimal.ZERO) continue

                val typeStr = i18nService.getMessage("transaction.type.${t.type.name.lowercase()}", locale, t.type.name.lowercase())
                val amountClass = if (t.amount >= BigDecimal.ZERO) "amount-in" else "amount-out"
                val amountFormatted = String.format("%,.2f", t.amount.abs().toDouble())
                val categoryName = t.category?.categoryName ?: "-"
                val dateStr = t.transactionDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd h:mm a", locale))
                val noteStr = t.note ?: ""

                val rowHtml = if (lang == Language.AR) {
                    """
                        <tr>
                            <td>$noteStr</td>
                            <td>$dateStr</td>
                            <td>$categoryName</td>
                            <td class="$amountClass">$amountFormatted</td>
                            <td>${t.title}</td>
                            <td>$typeStr</td>
                        </tr>
                    """.trimIndent()
                } else {
                    """
                        <tr>
                            <td>$typeStr</td>
                            <td>${t.title}</td>
                            <td class="$amountClass">$amountFormatted</td>
                            <td>$categoryName</td>
                            <td>$dateStr</td>
                            <td>$noteStr</td>
                        </tr>
                    """.trimIndent()
                }
                htmlContent.append(rowHtml)
            }

            hasMore = transactions.hasNext()
            page++
        }

        htmlContent.append("""
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent())

        val os = ByteArrayOutputStream()
        val builder = PdfRendererBuilder()
        builder.useFastMode()
        builder.useUnicodeBidiSplitter(ICUBidiSplitter.ICUBidiSplitterFactory())
        builder.useUnicodeBidiReorderer(ICUBidiReorderer())
        if (lang == Language.AR) {
            builder.defaultTextDirection(TextDirection.RTL)
        }
        
        val amiriRegularUrl = javaClass.getResource("/fonts/Amiri-Regular.ttf")
        val amiriBoldUrl = javaClass.getResource("/fonts/Amiri-Bold.ttf")
        val robotoRegularUrl = javaClass.getResource("/fonts/Roboto-Regular.ttf")
        val robotoBoldUrl = javaClass.getResource("/fonts/Roboto-Bold.ttf")
        
        if (amiriRegularUrl != null) {
            builder.useFont(File(amiriRegularUrl.toURI()), "Amiri")
        }
        if (amiriBoldUrl != null) {
            builder.useFont(File(amiriBoldUrl.toURI()), "Amiri", 700, com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL, true)
        }
        if (robotoRegularUrl != null) {
            builder.useFont(File(robotoRegularUrl.toURI()), "Roboto")
        }
        if (robotoBoldUrl != null) {
            builder.useFont(File(robotoBoldUrl.toURI()), "Roboto", 700, com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL, true)
        }

        builder.withHtmlContent(htmlContent.toString(), null)
        builder.toStream(os)
        builder.run()
        return os.toByteArray()
    }
}
