package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.entities.WishlistItem
import com.example.data.local.entities.WishlistStatus
import com.example.data.nepali.NepaliDateConverter
import com.example.data.repository.WishlistItemWithAffordability
import com.example.ui.components.AffordabilityBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PriorityIndicator
import com.example.ui.components.formatAmount
import com.example.ui.dialogs.AccountPicker
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun WishlistScreen(
    viewModel: FinanceViewModel,
    onOpenAddWishlist: () -> Unit
) {
    val wishlistItems by viewModel.wishlistWithAffordability.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val currency by viewModel.currencyState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Planned, 1 = Purchased, 2 = All
    val filtered = when (selectedTab) {
        0 -> wishlistItems.filter { it.item.status == WishlistStatus.PLANNED.name }
        1 -> wishlistItems.filter { it.item.status == WishlistStatus.PURCHASED.name }
        else -> wishlistItems
    }

    val totalCost = wishlistItems.filter { it.item.status == WishlistStatus.PLANNED.name }.sumOf { it.item.estimatedCost }
    val totalLiquid = accounts.filter { it.type != "INVESTMENT" }.sumOf { it.balance }

    var itemToPurchase by remember { mutableStateOf<WishlistItem?>(null) }
    var createExpenseChecked by remember { mutableStateOf(true) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var itemToDelete by remember { mutableStateOf<WishlistItem?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.wishlist_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.total_planned_cost), style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = formatAmount(totalCost, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalCost > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                            if (totalCost <= 0.0) {
                                Text(
                                    text = stringResource(R.string.no_items_yet),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.liquid_cash_available), style = MaterialTheme.typography.labelSmall)
                            Text(
                                formatAmount(totalLiquid, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalLiquid >= totalCost) Color(0xFF059669) else Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            // Tabs
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.planned)) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.purchased)) })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(stringResource(R.string.all)) })
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = stringResource(R.string.wishlist_clear),
                        subtitle = stringResource(R.string.wishlist_clear_description),
                        actionLabel = stringResource(R.string.add_wishlist_item),
                        onAction = onOpenAddWishlist,
                        icon = "🎯",
                        accentColor = EmeraldPrimary
                    )
                }
            } else {
                items(filtered, key = { it.item.id }) { itemAff ->
                    WishlistCardItem(
                        itemAff = itemAff,
                        currency = currency,
                        onPurchase = {
                            itemToPurchase = itemAff.item
                            createExpenseChecked = true
                            selectedAccountId = accounts.firstOrNull()?.id
                        },
                        onDelete = { itemToDelete = itemAff.item }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

    // Purchase Dialog
    itemToPurchase?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToPurchase = null },
            title = { Text(stringResource(R.string.mark_purchased)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.purchased_for, item.name, formatAmount(item.estimatedCost, currency)))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = createExpenseChecked,
                            onCheckedChange = { createExpenseChecked = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.create_expense_transaction), style = MaterialTheme.typography.bodyMedium)
                    }

                    if (createExpenseChecked) {
                        AccountPicker(
                            label = stringResource(R.string.debit_from_account),
                            accounts = accounts,
                            selectedId = selectedAccountId,
                            onSelect = { selectedAccountId = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purchaseWishlistItem(item, createExpenseChecked, selectedAccountId)
                        itemToPurchase = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToPurchase = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.delete_wishlist_item)) },
            text = { Text(stringResource(R.string.delete_wishlist_confirmation, item.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWishlistItem(item)
                        itemToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun WishlistCardItem(
    itemAff: WishlistItemWithAffordability,
    currency: String,
    onPurchase: () -> Unit,
    onDelete: () -> Unit
) {
    val item = itemAff.item

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formatAmount(item.estimatedCost, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityIndicator(priority = item.priority)
                AffordabilityBadge(affordability = itemAff.affordability)
            }

            if (itemAff.savingsNeeded > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.savings_needed, formatAmount(itemAff.savingsNeeded, currency)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444)
                )
            }

            item.preferredDate?.let { date ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.target_date, NepaliDateConverter.formatDualDate(date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item.notes?.let { n ->
                if (n.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = n, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.status == WishlistStatus.PLANNED.name) {
                    Button(
                        onClick = onPurchase,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.mark_purchased_short), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
