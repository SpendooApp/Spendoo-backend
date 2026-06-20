package org.spendoo.statistics.util

import com.openhtmltopdf.bidi.support.ICUBidiReorderer
import com.openhtmltopdf.bidi.support.ICUBidiSplitter
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.TextDirection
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.spendoo.i18n.I18nService
import org.spendoo.statistics.model.*
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.repository.TransactionViewRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// TODO: Try to make it in AI service if possible
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
        budgetRepository: BudgetRepository
    ): ByteArray {
        val themeClass = if (theme == Theme.DARK) "dark-theme" else ""
        val directionClass = if (lang == Language.AR) "rtl" else ""
        val dirAttr = if (lang == Language.AR) "rtl" else "ltr"
        val locale = lang.locale

        val formatter = DateTimeFormatter.ofPattern("MMM dd yyyy")
        val periodStr = "${startDate.format(formatter)} - ${endDate.format(formatter)}"
        
        val income = transactionRepository.sumIncomeByUserIdAndDateRange(userId, startDate, endDate)
        val expenses = transactionRepository.sumExpensesByUserIdAndDateRange(userId, startDate, endDate).negate()
        val saved = income.subtract(expenses)

        val htmlContent = StringBuilder()
        htmlContent.append("""
            <!DOCTYPE html>
            <html dir="$dirAttr">
            <head>
                <meta charset="utf-8"/>
                <style>
                    @page { margin: 30px; size: A4 portrait; }
                    body {
                        font-family: 'Roboto', 'DejaVu Sans', sans-serif;
                        font-size: 12px;
                        color: #1E293B;
                        background-color: #F8FAFC;
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
                    <h1>Detailed Report</h1>
                    <div class="summary">
                        <div>Total earnings: $%,.2f</div>
                        <div>Total spending: $%,.2f</div>
                        <div>Total saved: $%,.2f</div>
                        <div style="margin-top: 15px; font-weight: bold;">Report Period</div>
                        <div>$periodStr</div>
                    </div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Type</th>
                            <th>Title</th>
                            <th>Amount</th>
                            <th>Category</th>
                            <th>Date</th>
                            <th>Note</th>
                        </tr>
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

                val typeStr = t.type.name.lowercase()
                val amountClass = if (t.amount >= BigDecimal.ZERO) "amount-in" else "amount-out"
                val amountFormatted = String.format("%,.2f", t.amount.abs().toDouble())
                val categoryName = t.category?.categoryName ?: "-"
                val dateStr = t.transactionDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd h:mm a"))
                val noteStr = t.note ?: ""

                htmlContent.append("""
                    <tr>
                        <td>$typeStr</td>
                        <td>${t.title}</td>
                        <td class="$amountClass">$amountFormatted</td>
                        <td>$categoryName</td>
                        <td>$dateStr</td>
                        <td>$noteStr</td>
                    </tr>
                """.trimIndent())
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
        val regularFontUrl = javaClass.getResource("/fonts/Roboto-Regular.ttf")
        val boldFontUrl = javaClass.getResource("/fonts/Roboto-Bold.ttf")
        
        if (regularFontUrl != null) {
            builder.useFont(File(regularFontUrl.toURI()), "Roboto")
        }
        if (boldFontUrl != null) {
            builder.useFont(File(boldFontUrl.toURI()), "Roboto", 700, com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL, true)
        }

        builder.withHtmlContent(htmlContent.toString(), null)
        builder.toStream(os)
        builder.run()
        return os.toByteArray()
    }
}
