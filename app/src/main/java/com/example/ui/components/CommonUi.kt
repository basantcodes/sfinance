package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
