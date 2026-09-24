package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.TransactionEntity
import com.example.data.nepali.NepaliDateConverter
import com.example.ui.components.ExpensePieChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IncomeExpenseAreaChart
import com.example.ui.components.TransactionTypeBadge
import com.example.ui.components.formatAmount
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TealDark
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToLoans: () -> Unit,
    onOpenAddTransaction: () -> Unit
) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val currency by viewModel.currencyState.collectAsState()
    val year by viewModel.dashboardYear.collectAsState()
    val month by viewModel.dashboardMonth.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryMap = categories.associateBy { it.id }
    val authState by viewModel.authState.collectAsState()

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val userName = authState.currentUser?.name?.trim()?.ifBlank { null } ?: "there"
    val greetingTitle = "$greeting, $userName"

    val monthName = SimpleDateFormat("MMMM", Locale.US).apply {
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, month - 1) }
    }.format(Calendar.getInstance().apply { set(Calendar.MONTH, month - 1) }.time)

    val todayDualDate = NepaliDateConverter.formatDualDate(System.currentTimeMillis())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Today: $todayDualDate",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = greetingTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (month == 1) viewModel.setDashboardPeriod(year - 1, 12)
                                else viewModel.setDashboardPeriod(year, month - 1)
                            }
                        ) {
                            Icon(
                                Icons.Default.ArrowBackIosNew,
                                contentDescription = "Previous Month",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$monthName $year",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                if (month == 12) viewModel.setDashboardPeriod(year + 1, 1)
                                else viewModel.setDashboardPeriod(year, month + 1)
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Next Month",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

            // CARD 1: Net Worth Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(EmeraldPrimary, TealDark)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "NET WORTH",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatAmount(dashboardData.netWorth, currency),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 14.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                thickness = 1.dp
                            )

                            // Spacious 2x2 Grid with 12dp spacing
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        AccountMiniSummary(
                                            icon = Icons.Default.AccountBalance,
                                            label = "Bank",
                                            amount = formatAmount(dashboardData.totalBank, currency)
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        AccountMiniSummary(
                                            icon = Icons.Default.CreditCard,
                                            label = "Cash & Wallet",
                                            amount = formatAmount(dashboardData.totalCash + dashboardData.totalWallet, currency)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        AccountMiniSummary(
                                            icon = Icons.Default.Savings,
                                            label = "Investments",
                                            amount = formatAmount(dashboardData.totalInvestments, currency)
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        AccountMiniSummary(
                                            icon = Icons.Default.Wallet,
                                            label = "Active Loans",
                                            amount = "${dashboardData.activeLoansCount} active"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CARD 2: Monthly Performance & Cash Flow Analytics Card (Consolidated)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Monthly Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlowItem(
                                title = "Income",
                                amount = formatAmount(dashboardData.monthlyIncome, currency),
                                color = Color(0xFF059669),
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowItem(
                                title = "Expenses",
                                amount = formatAmount(dashboardData.monthlyExpenses, currency),
                                color = Color(0xFFEF4444),
                                icon = Icons.AutoMirrored.Filled.TrendingDown,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowItem(
                                title = "Saved",
                                amount = formatAmount(dashboardData.monthlySaved, currency),
                                color = if (dashboardData.monthlySaved >= 0) Color(0xFF0D9488) else Color(0xFFEF4444),
                                icon = Icons.Default.Wallet,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val savingsProgress = (dashboardData.savingsRate.toFloat() / 100f).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Savings Rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", dashboardData.savingsRate)}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (dashboardData.savingsRate >= 0) Color(0xFF059669) else Color(0xFFEF4444)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { savingsProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (dashboardData.savingsRate >= 0) EmeraldPrimary else Color(0xFFEF4444),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        Text(
                            text = "6-Month Cash Flow Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        IncomeExpenseAreaChart(
                            trendData = dashboardData.incomeExpenseTrend,
                            currency = currency
                        )

                        if (dashboardData.expenseByCategory.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                            Text(
                                text = "Expenses by Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ExpensePieChart(
                                expenses = dashboardData.expenseByCategory,
                                currency = currency
                            )
                        }
                    }
                }
            }

            // CARD 3: Budgets & Debt Position Card (Consolidated)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Section 1: Monthly Budgets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Budgets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToBudgets) {
                                Text("Manage →", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (dashboardData.budgetsWithProgress.isEmpty()) {
                            EmptyStateCard(
                                title = "No monthly budgets yet",
                                subtitle = "Add category limits to turn this section into a live spending guide.",
                                actionLabel = "Manage Budgets",
                                onAction = onNavigateToBudgets,
                                icon = "📊",
                                accentColor = EmeraldPrimary
                            )
                        } else {
                            dashboardData.budgetsWithProgress.take(3).forEach { bp ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(bp.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${formatAmount(bp.spent, currency)} / ${formatAmount(bp.limit, currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (bp.isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { bp.percentage.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (bp.isOverBudget) Color(0xFFEF4444) else if (bp.percentage > 0.8f) Color(0xFFF59E0B) else Color(0xFF10B981),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        // Section 2: Loan Position Glance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Loan Position (${dashboardData.activeLoansCount} Active)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToLoans) {
                                Text("Details →", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("You'll receive", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                                Text(formatAmount(dashboardData.totalLent, currency), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("You'll pay", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9333EA))
                                Text(formatAmount(dashboardData.totalBorrowed, currency), fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Net", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatAmount(dashboardData.netReceivable, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = if (dashboardData.netReceivable >= 0) Color(0xFF059669) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }

            // CARD 4: Recent Transactions Card (Consolidated)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToTransactions) {
                                Text("View All", color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (dashboardData.recentTransactions.isEmpty()) {
                            EmptyStateCard(
                                title = "No recent activity",
                                subtitle = "Add your first transaction and your dashboard will start telling the story.",
                                actionLabel = "Add Transaction",
                                onAction = onOpenAddTransaction,
                                icon = "🧾",
                                accentColor = EmeraldPrimary
                            )
                        } else {
                            dashboardData.recentTransactions.take(5).forEach { txn ->
                                RecentTransactionItem(
                                    transaction = txn,
                                    currency = currency,
                                    categoryName = categoryMap[txn.categoryId]?.name
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun AccountMiniSummary(icon: ImageVector, label: String, amount: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = amount,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun FlowItem(
    title: String,
    amount: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.12f),
                    tonalElevation = 0.dp
                ) {
                    Box(modifier = Modifier.padding(6.dp)) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentTransactionItem(
    transaction: TransactionEntity,
    currency: String,
    categoryName: String?
) {
    val balanceColor = when (transaction.type) {
        "INCOME" -> Color(0xFF059669)
        "EXPENSE" -> Color(0xFFEF4444)
        "LEND" -> Color(0xFFD97706)
        "BORROW" -> Color(0xFF9333EA)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                TransactionTypeBadge(type = transaction.type)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = transaction.name ?: categoryName ?: transaction.type,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${SimpleDateFormat("MMM dd", Locale.US).format(Date(transaction.date))} (${transaction.dateBs})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isPositive = transaction.type == "INCOME" || transaction.type == "BORROW"
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = balanceColor.copy(alpha = 0.12f),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "${if (isPositive) "+" else "-"}${formatAmount(transaction.amount, currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
