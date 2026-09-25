package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.local.entities.JournalEntry
import com.example.data.local.entities.Mood
import com.example.data.nepali.NepaliDateConverter
import com.example.ui.components.FormFeedbackMessage
import com.example.ui.components.NepaliDatePickerDialog

@Composable
fun JournalDialog(
    entryToEdit: JournalEntry? = null,
    onDismissRequest: () -> Unit,
    onSaveEntry: (content: String, mood: String?, date: Long) -> Unit
) {
    var content by remember { mutableStateOf(entryToEdit?.content ?: "") }
    var selectedMood by remember { mutableStateOf(entryToEdit?.mood ?: Mood.HAPPY.name) }
    var selectedDate by remember { mutableLongStateOf(entryToEdit?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val journalEmptyErrorMessage = stringResource(R.string.journal_empty_error)

    val moods = listOf(
        Mood.HAPPY.name to "😊",
        Mood.GRATEFUL.name to "🙏",
        Mood.NEUTRAL.name to "😐",
        Mood.ANXIOUS.name to "😰",
        Mood.SAD.name to "😢"
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.94f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        title = {
            Text(
                text = stringResource(if (entryToEdit == null) R.string.new_journal_entry else R.string.edit_journal_entry),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mood selector
                Text(text = stringResource(R.string.feeling_prompt), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    moods.forEach { (mood, emoji) ->
                        val isSelected = selectedMood == mood
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedMood = mood }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mood.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Date Picker trigger
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.date_dual), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(NepaliDateConverter.formatDualDate(selectedDate), fontWeight = FontWeight.SemiBold)
                        }
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.pick_date), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Journal Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.write_thoughts)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                    maxLines = 10
                )

                errorMessage?.let {
                    FormFeedbackMessage(message = it, isError = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isBlank()) {
                        errorMessage = journalEmptyErrorMessage
                        return@Button
                    }
                    onSaveEntry(content.trim(), selectedMood, selectedDate)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        NepaliDatePickerDialog(
            initialDateMillis = selectedDate,
            onDateSelected = { millis, _ ->
                selectedDate = millis
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }
}
