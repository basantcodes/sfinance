package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.local.entities.Account
import com.example.data.local.entities.Category
import com.example.data.local.entities.TransactionEntity
import com.example.data.nepali.NepaliDateConverter
import com.example.data.repository.DashboardData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateStatementPdf(
        context: Context,
        dashboardData: DashboardData,
        transactions: List<TransactionEntity>,
        accounts: List<Account>,
        categories: Map<String, Category>,
        currency: String
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#047857")
            textSize = 20f
            isFakeBoldText = true
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0f172a")
            textSize = 14f
            isFakeBoldText = true
        }
        val subheaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748b")
            textSize = 10f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 10f
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1e293b")
            textSize = 10f
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#e2e8f0")
            strokeWidth = 1f
        }

        var y = 45f

        // Top Banner / Title
        paint.color = Color.parseColor("#ECFDF5")
        canvas.drawRoundRect(28f, 24f, 567f, 106f, 14f, 14f, paint)
        paint.color = Color.parseColor("#10B981")
        canvas.drawRoundRect(28f, 24f, 36f, 106f, 4f, 4f, paint)
        canvas.drawText(context.getString(com.example.R.string.app_name), 40f, y, titlePaint)
        y += 18f
        val now = System.currentTimeMillis()
        val dualDateStr = NepaliDateConverter.formatDualDate(now)
        canvas.drawText(context.getString(com.example.R.string.pdf_generated_statement, dualDateStr), 40f, y, subheaderPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_offline_snapshot), 425f, y, subheaderPaint)
        y += 20f
        canvas.drawText(context.getString(com.example.R.string.pdf_accounts_activity), 40f, y, subheaderPaint)
        y += 24f

        // Summary Card Box
        paint.color = Color.parseColor("#f8fafc")
        canvas.drawRoundRect(40f, y, 555f, y + 88f, 10f, 10f, paint)

        canvas.drawText(context.getString(com.example.R.string.pdf_financial_overview, currency), 55f, y + 22f, headerPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_net_worth, currency, String.format(Locale.US, "%.2f", dashboardData.netWorth)), 55f, y + 42f, boldBodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_bank, currency, String.format(Locale.US, "%.2f", dashboardData.totalBank)), 55f, y + 60f, bodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_cash_wallet, currency, String.format(Locale.US, "%.2f", dashboardData.totalCash + dashboardData.totalWallet)), 220f, y + 42f, bodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_investments, currency, String.format(Locale.US, "%.2f", dashboardData.totalInvestments)), 220f, y + 60f, bodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_total_lent, currency, String.format(Locale.US, "%.2f", dashboardData.totalLent)), 390f, y + 42f, bodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_total_borrowed, currency, String.format(Locale.US, "%.2f", dashboardData.totalBorrowed)), 390f, y + 60f, bodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_transactions, transactions.size), 390f, y + 78f, bodyPaint)

        y += 113f

        // Transactions Table Header
        canvas.drawText(context.getString(com.example.R.string.pdf_recent_transactions), 40f, y, headerPaint)
        y += 14f

        paint.color = Color.parseColor("#f1f5f9")
        canvas.drawRect(40f, y, 555f, y + 20f, paint)
        canvas.drawText(context.getString(com.example.R.string.pdf_date), 45f, y + 14f, boldBodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_type), 185f, y + 14f, boldBodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_description_column), 250f, y + 14f, boldBodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_category), 390f, y + 14f, boldBodyPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_amount), 485f, y + 14f, boldBodyPaint)
        y += 22f

        val accountMap = accounts.associateBy { it.id }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val txnsToPrint = transactions.take(24) // Fit cleanly on single page
        for ((index, txn) in txnsToPrint.withIndex()) {
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(40f, y, 555f, y + 20f, paint)
            }
            val dateStr = "${dateFormat.format(Date(txn.date))} (${txn.dateBs})"
            val typeStr = when (txn.type) {
                "INCOME" -> context.getString(com.example.R.string.income)
                "EXPENSE" -> context.getString(com.example.R.string.expense)
                "TRANSFER" -> context.getString(com.example.R.string.transfer)
                "LEND" -> context.getString(com.example.R.string.lend)
                "BORROW" -> context.getString(com.example.R.string.borrow)
                else -> txn.type
            }
            val desc = (txn.name ?: "Transaction").take(22)
            val catStr = (categories[txn.categoryId]?.name ?: "-").take(16)
            val amountStr = "$currency ${String.format(Locale.US, "%.2f", txn.amount)}"

            canvas.drawText(dateStr, 45f, y + 13f, bodyPaint)

            // Color coding for amount
            val amtPaint = Paint(boldBodyPaint)
            when (txn.type) {
                "INCOME" -> amtPaint.color = Color.parseColor("#059669")
                "EXPENSE" -> amtPaint.color = Color.parseColor("#ef4444")
                "LEND" -> amtPaint.color = Color.parseColor("#d97706")
                "BORROW" -> amtPaint.color = Color.parseColor("#9333ea")
                else -> amtPaint.color = Color.parseColor("#3b82f6")
            }

            canvas.drawText(typeStr, 185f, y + 13f, bodyPaint)
            canvas.drawText(desc, 250f, y + 13f, bodyPaint)
            canvas.drawText(catStr, 390f, y + 13f, bodyPaint)
            canvas.drawText(amountStr, 485f, y + 13f, amtPaint)

            y += 20f
            canvas.drawLine(40f, y, 555f, y, linePaint)
        }

        // Footer
        canvas.drawLine(40f, 790f, 555f, 790f, linePaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_footer), 40f, 815f, subheaderPaint)
        canvas.drawText(context.getString(com.example.R.string.pdf_generated_at, SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(now))), 405f, 815f, subheaderPaint)

        document.finishPage(page)

        val outputFile = File(context.cacheDir, "finance_statement_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return outputFile
    }
}
