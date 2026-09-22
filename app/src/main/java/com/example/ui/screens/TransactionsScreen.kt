package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.TransactionEntity
import com.example.data.nepali.NepaliDateConverter
import com.example.ui.components.NepaliDatePickerDialog
import com.example.ui.components.TransactionTypeBadge
import com.example.ui.components.formatAmount
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    onOpenAddTransaction: () -> Unit,
    onOpenEditTransaction: (TransactionEntity) -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currency by viewModel.currencyState.collectAsState()

    val selectedType by viewModel.transactionFilterType.collectAsState()
    val searchQuery by viewModel.transactionSearchQuery.collectAsState()

    val accountMap = accounts.associateBy { it.id }
    val categoryMap = categories.associateBy { it.id }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var customDateRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val filterTypes = listOf("ALL", "EXPENSE", "INCOME", "TRANSFER", "LEND", "BORROW")
    val pageSizeOptions = listOf("10", "20", "30", "All")
    var selectedPageSize by remember { mutableStateOf("20") }
    var visibleLimit by remember { mutableIntStateOf(20) }

    // Combine filters: Type (from viewModel), Search (from viewModel), Custom Date Range, and Category Filter
    val combinedFilteredTransactions = remember(transactions, customDateRange, selectedCategoryId) {
        var list = transactions
        if (customDateRange != null) {
            val start = customDateRange!!.first
            val end = customDateRange!!.second
            list = list.filter { it.date in start..end }
        }
        if (selectedCategoryId != null) {
            list = list.filter { it.categoryId == selectedCategoryId }
        }
        list
    }

    // Reset pagination when filters change
    LaunchedEffect(selectedPageSize, combinedFilteredTransactions.size, selectedCategoryId, selectedType, searchQuery, customDateRange) {
        visibleLimit = when (selectedPageSize) {
            "10" -> 10
            "20" -> 20
            "30" -> 30
            "All" -> combinedFilteredTransactions.size
            else -> 20
        }
    }

    val visibleTransactions = remember(combinedFilteredTransactions, visibleLimit, selectedPageSize) {
        if (selectedPageSize == "All") {
            combinedFilteredTransactions
        } else {
            combinedFilteredTransactions.take(visibleLimit)
        }
    }

    // Top-level grouping by Month, then day-level grouping within each month
    val monthlyGrouped = remember(visibleTransactions) {
        visibleTransactions.groupBy { txn ->
            val bs = NepaliDateConverter.adToBs(txn.date)
            val gregorianMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(txn.date))
            val bsMonthName = NepaliDateConverter.nepaliMonths.getOrElse(bs.month - 1) { "" }
            "$gregorianMonth • $bsMonthName ${bs.year} BS"
        }.mapValues { (_, txnsInMonth) ->
            txnsInMonth.groupBy { txn ->
                NepaliDateConverter.formatDualDate(txn.date)
            }
        }
    }

    // Collapsed month & date-group state (tap to fold/unfold)
    val collapsedMonths = remember { mutableStateMapOf<String, Boolean>() }
    val collapsedDates = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search & Filter
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.transactionSearchQuery.value = it },
                placeholder = { Text("Search by name, notes, or BS date...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.transactionSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Row 1: Transaction Type Chips with Custom Date Range
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterTypes) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { viewModel.transactionFilterType.value = type },
                        label = { Text(type) }
                    )
                }
                item {
                    val isRangeActive = customDateRange != null
                    FilterChip(
                        selected = isRangeActive,
                        onClick = { showCustomRangeDialog = true },
                        label = {
                            if (isRangeActive) {
                                val startBs = NepaliDateConverter.adToBs(customDateRange!!.first).formatted
                                val endBs = NepaliDateConverter.adToBs(customDateRange!!.second).formatted
                                Text("$startBs - $endBs")
                            } else {
                                Text("Custom Range")
                            }
                        },
                        trailingIcon = if (isRangeActive) {
                            {
                                IconButton(
                                    onClick = { customDateRange = null },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear range",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Category Filter Chips (combine with type filters)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("All Categories") }
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Page Size Controls & Count Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Showing ${visibleTransactions.size} of ${combinedFilteredTransactions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Page Size:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    pageSizeOptions.forEach { sizeOption ->
                        val isSelected = selectedPageSize == sizeOption
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedPageSize = sizeOption
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = sizeOption,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (combinedFilteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No transactions match your criteria.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onOpenAddTransaction) {
                        Text("+ Add First Transaction", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                monthlyGrouped.forEach { (monthHeader, daysMap) ->
                    val isMonthCollapsed = collapsedMonths[monthHeader] == true
                    val totalTxnsInMonth = daysMap.values.sumOf { it.size }
                    val monthIncome = daysMap.values.flatten().filter { it.type == "INCOME" }.sumOf { it.amount }
                    val monthExpense = daysMap.values.flatten().filter { it.type == "EXPENSE" }.sumOf { it.amount }

                    // Top-Level Month Section Header
                    stickyHeader(key = "month_$monthHeader") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { collapsedMonths[monthHeader] = !isMonthCollapsed },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 3.dp,
                            shadowElevation = 2.dp,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = monthHeader,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "($totalTxnsInMonth)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (monthIncome > 0 || monthExpense > 0) {
                                        Row(
                                            modifier = Modifier.padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (monthIncome > 0) {
                                                Text(
                                                    text = "+${formatAmount(monthIncome, currency)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            if (monthExpense > 0) {
                                                Text(
                                                    text = "-${formatAmount(monthExpense, currency)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = if (isMonthCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (isMonthCollapsed) "Expand Month" else "Collapse Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!isMonthCollapsed) {
                        daysMap.forEach { (dateHeader, txns) ->
                            val isDayCollapsed = collapsedDates[dateHeader] == true
                            item(key = "day_header_${monthHeader}_$dateHeader") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { collapsedDates[dateHeader] = !isDayCollapsed },
                                    color = MaterialTheme.colorScheme.background,
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = dateHeader,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${txns.size})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isDayCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                            contentDescription = if (isDayCollapsed) "Expand Day" else "Collapse Day",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (!isDayCollapsed) {
                                items(txns, key = { it.id }) { txn ->
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            when (value) {
                                                SwipeToDismissBoxValue.StartToEnd -> {
                                                    onOpenEditTransaction(txn)
                                                    false
                                                }
                                                SwipeToDismissBoxValue.EndToStart -> {
                                                    transactionToDelete = txn
                                                    false
                                                }
                                                SwipeToDismissBoxValue.Settled -> false
                                            }
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = {
                                            val direction = dismissState.dismissDirection
                                            val color = when (direction) {
                                                SwipeToDismissBoxValue.StartToEnd -> EmeraldPrimary
                                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                                else -> Color.Transparent
                                            }
                                            val alignment = when (direction) {
                                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                                else -> Alignment.Center
                                            }
                                            val icon = when (direction) {
                                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                                else -> Icons.Default.Edit
                                            }
                                            val label = when (direction) {
                                                SwipeToDismissBoxValue.StartToEnd -> "Edit"
                                                SwipeToDismissBoxValue.EndToStart -> "Delete"
                                                else -> ""
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(color)
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = alignment
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(icon, contentDescription = label, tint = Color.White)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        },
                                        content = {
                                            TransactionRowCard(
                                                transaction = txn,
                                                currency = currency,
                                                accountFrom = txn.accountFromId?.let { accountMap[it]?.name },
                                                accountTo = txn.accountToId?.let { accountMap[it]?.name },
                                                categoryName = txn.categoryId?.let { categoryMap[it]?.name },
                                                onClick = { onOpenEditTransaction(txn) }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Pagination / Lazy-load control at bottom
                if (selectedPageSize != "All" && visibleLimit < combinedFilteredTransactions.size) {
                    item(key = "load_more_button") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val step = selectedPageSize.toIntOrNull() ?: 20
                                    visibleLimit = (visibleLimit + step).coerceAtMost(combinedFilteredTransactions.size)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Load More (${combinedFilteredTransactions.size - visibleLimit} remaining)")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomRangeDialog) {
        var tempStartMillis by remember {
            mutableStateOf(customDateRange?.first ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
        }
        var tempEndMillis by remember {
            mutableStateOf(customDateRange?.second ?: System.currentTimeMillis())
        }
        var pickingFor by remember { mutableStateOf<String?>(null) }

        if (pickingFor != null) {
            NepaliDatePickerDialog(
                initialDateMillis = if (pickingFor == "START") tempStartMillis else tempEndMillis,
                onDateSelected = { millis, _ ->
                    if (pickingFor == "START") {
                        tempStartMillis = millis
                    } else {
                        tempEndMillis = millis
                    }
                    pickingFor = null
                },
                onDismissRequest = { pickingFor = null }
            )
        }

        Dialog(
            onDismissRequest = { showCustomRangeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Filter by Custom Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "From Date (BS / AD)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { pickingFor = "START" },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = NepaliDateConverter.formatDualDate(tempStartMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Start Date",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "To Date (BS / AD)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { pickingFor = "END" },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = NepaliDateConverter.formatDualDate(tempEndMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select End Date",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                customDateRange = null
                                showCustomRangeDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear")
                        }
                        Button(
                            onClick = {
                                val (s, e) = if (tempStartMillis <= tempEndMillis) {
                                    tempStartMillis to tempEndMillis
                                } else {
                                    tempEndMillis to tempStartMillis
                                }
                                val endCal = Calendar.getInstance().apply {
                                    timeInMillis = e
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }
                                val startCal = Calendar.getInstance().apply {
                                    timeInMillis = s
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                customDateRange = Pair(startCal.timeInMillis, endCal.timeInMillis)
                                showCustomRangeDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    transactionToDelete?.let { txn ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = {
                Text("Are you sure you want to delete this ${txn.type} of $currency ${String.format("%.2f", txn.amount)}? Account balances will be safely reversed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(txn)
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionRowCard(
    transaction: TransactionEntity,
    currency: String,
    accountFrom: String?,
    accountTo: String?,
    categoryName: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                TransactionTypeBadge(type = transaction.type)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.name ?: categoryName ?: transaction.type,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val contextDesc = when (transaction.type) {
                        "TRANSFER" -> "${accountFrom ?: "?"} → ${accountTo ?: "?"}"
                        "INCOME" -> "To: ${accountTo ?: "Account"}" + (categoryName?.let { " • $it" } ?: "")
                        "EXPENSE" -> "From: ${accountFrom ?: "Account"}" + (categoryName?.let { " • $it" } ?: "")
                        "LEND" -> "From: ${accountFrom ?: "Account"}"
                        "BORROW" -> "To: ${accountTo ?: "Account"}"
                        else -> categoryName ?: ""
                    }
                    Text(
                        text = contextDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    transaction.notes?.let { notes ->
                        if (notes.isNotBlank()) {
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isPositive = transaction.type == "INCOME" || transaction.type == "BORROW"
                Text(
                    text = "${if (isPositive) "+" else "-"}${formatAmount(transaction.amount, currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        "INCOME" -> Color(0xFF059669)
                        "EXPENSE" -> Color(0xFFEF4444)
                        "LEND" -> Color(0xFFD97706)
                        "BORROW" -> Color(0xFF9333EA)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
