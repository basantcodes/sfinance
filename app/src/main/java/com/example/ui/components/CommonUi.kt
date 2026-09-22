package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.Mood
import com.example.data.local.entities.TransactionType
import com.example.data.repository.Affordability
import com.example.ui.theme.ColorBorrow
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.ColorLend
import com.example.ui.theme.ColorTransfer
import java.util.Locale

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: String = "📌",
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 28.sp)
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
fun LoadingStateCard(
    title: String = "Loading your data",
    subtitle: String = "Checking balances and recent activity…"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp, 18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FormFeedbackMessage(
    message: String,
    isError: Boolean = false,
    isSuccess: Boolean = false
) {
    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isSuccess -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val textColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when {
        isError -> Icons.Outlined.WarningAmber
        isSuccess -> Icons.Default.CheckCircle
        else -> Icons.Default.Info
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TransactionTypeBadge(type: String, modifier: Modifier = Modifier) {
    val (bg, fg, emoji) = when (type) {
        TransactionType.INCOME.name -> Triple(ColorIncome.copy(alpha = 0.15f), ColorIncome, "↓")
        TransactionType.EXPENSE.name -> Triple(ColorExpense.copy(alpha = 0.15f), ColorExpense, "↑")
        TransactionType.TRANSFER.name -> Triple(ColorTransfer.copy(alpha = 0.15f), ColorTransfer, "⇄")
        TransactionType.LEND.name -> Triple(ColorLend.copy(alpha = 0.15f), ColorLend, "↗")
        TransactionType.BORROW.name -> Triple(ColorBorrow.copy(alpha = 0.15f), ColorBorrow, "↘")
        else -> Triple(Color.Gray.copy(alpha = 0.15f), Color.Gray, "•")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = type, color = fg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun AffordabilityBadge(affordability: Affordability, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (affordability) {
        Affordability.CAN_AFFORD_NOW -> Triple(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF047857), "Can Afford Now")
        Affordability.CAN_AFFORD_BY_DATE -> Triple(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFB45309), "Projected by Date")
        Affordability.CANNOT_AFFORD -> Triple(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFB91C1C), "Need Savings")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = fg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
fun PriorityIndicator(priority: String, modifier: Modifier = Modifier) {
    val (color, label) = when (priority.uppercase()) {
        "HIGH" -> Color(0xFFEF4444) to "High"
        "MEDIUM" -> Color(0xFFF59E0B) to "Med"
        else -> Color(0xFF10B981) to "Low"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

fun getMoodEmoji(mood: String?): String {
    return when (mood?.uppercase()) {
        "HAPPY" -> "😊"
        "SAD" -> "😢"
        "ANXIOUS" -> "😰"
        "GRATEFUL" -> "🙏"
        "NEUTRAL" -> "😐"
        else -> "📝"
    }
}

fun formatAmount(amount: Double, currency: String): String {
    return "$currency ${String.format(Locale.US, "%,.2f", amount)}"
}
