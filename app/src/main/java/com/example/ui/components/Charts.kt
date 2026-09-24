package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.repository.CategoryExpenseShare
import com.example.data.repository.MonthlyTrendPoint
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensePieChart(
    expenses: List<CategoryExpenseShare>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.expense_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = expenses.sumOf { it.amount }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 32.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)
                var startAngle = -90f

                for (item in expenses) {
                    val sweepAngle = (item.percentage * 360f).coerceAtLeast(2f)
                    val parsedColor = runCatching { Color(android.graphics.Color.parseColor(item.color)) }
                        .getOrDefault(Color(0xFF10B981))

                    drawArc(
                        color = parsedColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2f, // subtle gap
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.total_spent_chart),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.currency_total, currency, String.format(Locale.US, "%.0f", total)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic legend
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            expenses.take(6).forEach { item ->
                val parsedColor = runCatching { Color(android.graphics.Color.parseColor(item.color)) }
                    .getOrDefault(Color(0xFF10B981))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(parsedColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.expense_percent, item.categoryName, String.format(Locale.US, "%.0f", item.percentage * 100)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseAreaChart(
    trendData: List<MonthlyTrendPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.trend_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxAmount = trendData.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val incomeColor = Color(0xFF059669)
    val expenseColor = Color(0xFFEF4444)

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(incomeColor, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.income), style = MaterialTheme.typography.labelSmall, color = incomeColor)

            Spacer(modifier = Modifier.width(16.dp))

            Box(modifier = Modifier.size(8.dp).background(expenseColor, CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = stringResource(R.string.expenses), style = MaterialTheme.typography.labelSmall, color = expenseColor)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp)
        ) {
            val width = size.width
            val height = size.height
            val count = trendData.size
            val stepX = if (count > 1) width / (count - 1) else width
            val plotTop = 8.dp.toPx()
            val plotBottom = height - 6.dp.toPx()
            val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)

            // Grid lines
            for (i in 0..3) {
                val y = plotTop + plotHeight * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Draw Income Path
            val incomePath = Path()
            val incomeAreaPath = Path()
            val expensePath = Path()
            val expenseAreaPath = Path()

            val incomePoints = mutableListOf<Offset>()
            val expensePoints = mutableListOf<Offset>()

            trendData.forEachIndexed { index, point ->
                val x = index * stepX
                val incY = (plotBottom - ((point.income / maxAmount).toFloat() * plotHeight)).coerceIn(plotTop, plotBottom)
                val expY = (plotBottom - ((point.expense / maxAmount).toFloat() * plotHeight)).coerceIn(plotTop, plotBottom)
                incomePoints.add(Offset(x, incY))
                expensePoints.add(Offset(x, expY))
            }

            // Build smooth Income curve
            if (incomePoints.isNotEmpty()) {
                incomePath.moveTo(incomePoints[0].x, incomePoints[0].y)
                incomeAreaPath.moveTo(incomePoints[0].x, plotBottom)
                incomeAreaPath.lineTo(incomePoints[0].x, incomePoints[0].y)

                for (i in 1 until incomePoints.size) {
                    val prev = incomePoints[i - 1]
                    val curr = incomePoints[i]
                    val cX1 = (prev.x + curr.x) / 2
                    val cY1 = prev.y
                    val cX2 = (prev.x + curr.x) / 2
                    val cY2 = curr.y
                    incomePath.cubicTo(cX1, cY1, cX2, cY2, curr.x, curr.y)
                    incomeAreaPath.cubicTo(cX1, cY1, cX2, cY2, curr.x, curr.y)
                }
                incomeAreaPath.lineTo(incomePoints.last().x, plotBottom)
                incomeAreaPath.close()

                drawPath(
                    path = incomeAreaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(incomeColor.copy(alpha = 0.35f), incomeColor.copy(alpha = 0.02f)),
                        startY = plotTop,
                        endY = plotBottom
                    ),
                    style = Fill
                )
                drawPath(
                    path = incomePath,
                    color = incomeColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                incomePoints.forEach { pt ->
                    drawCircle(color = incomeColor, radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                }
            }

            // Build smooth Expense curve
            if (expensePoints.isNotEmpty()) {
                expensePath.moveTo(expensePoints[0].x, expensePoints[0].y)
                expenseAreaPath.moveTo(expensePoints[0].x, plotBottom)
                expenseAreaPath.lineTo(expensePoints[0].x, expensePoints[0].y)

                for (i in 1 until expensePoints.size) {
                    val prev = expensePoints[i - 1]
                    val curr = expensePoints[i]
                    val cX1 = (prev.x + curr.x) / 2
                    val cY1 = prev.y
                    val cX2 = (prev.x + curr.x) / 2
                    val cY2 = curr.y
                    expensePath.cubicTo(cX1, cY1, cX2, cY2, curr.x, curr.y)
                    expenseAreaPath.cubicTo(cX1, cY1, cX2, cY2, curr.x, curr.y)
                }
                expenseAreaPath.lineTo(expensePoints.last().x, plotBottom)
                expenseAreaPath.close()

                drawPath(
                    path = expenseAreaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(expenseColor.copy(alpha = 0.25f), expenseColor.copy(alpha = 0.02f)),
                        startY = plotTop,
                        endY = plotBottom
                    ),
                    style = Fill
                )
                drawPath(
                    path = expensePath,
                    color = expenseColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                expensePoints.forEach { pt ->
                    drawCircle(color = expenseColor, radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                }
            }
        }

        // X-axis month labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trendData.forEach { pt ->
                Text(
                    text = pt.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
