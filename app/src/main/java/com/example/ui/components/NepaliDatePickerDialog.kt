package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.nepali.NepaliDateConverter
import java.util.Calendar

@Composable
fun NepaliDatePickerDialog(
    initialDateMillis: Long = System.currentTimeMillis(),
    onDateSelected: (millis: Long, bsString: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedMillis by remember { mutableStateOf(initialDateMillis) }
    var modeTab by remember { mutableIntStateOf(0) } // 0 = BS (Bikram Sambat), 1 = AD (Gregorian)

    val initialBs = remember(initialDateMillis) { NepaliDateConverter.adToBs(initialDateMillis) }
    val initialCal = remember(initialDateMillis) {
        Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    }

    var selectedBsYear by remember { mutableIntStateOf(initialBs.year) }
    var selectedBsMonth by remember { mutableIntStateOf(initialBs.month) }
    var selectedBsDay by remember { mutableIntStateOf(initialBs.day) }

    var selectedAdYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedAdMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH) + 1) }
    var selectedAdDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }

    // Synchronize year, month, day states whenever selectedMillis changes
    LaunchedEffect(selectedMillis) {
        val bsDate = NepaliDateConverter.adToBs(selectedMillis)
        selectedBsYear = bsDate.year
        selectedBsMonth = bsDate.month
        selectedBsDay = bsDate.day

        val c = Calendar.getInstance().apply { timeInMillis = selectedMillis }
        selectedAdYear = c.get(Calendar.YEAR)
        selectedAdMonth = c.get(Calendar.MONTH) + 1
        selectedAdDay = c.get(Calendar.DAY_OF_MONTH)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.select_date_dual),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = NepaliDateConverter.formatDualDate(selectedMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = modeTab) {
                    Tab(
                        selected = modeTab == 0,
                        onClick = { modeTab = 0 },
                        text = { Text(stringResource(R.string.bs_bikram_sambat)) }
                    )
                    Tab(
                        selected = modeTab == 1,
                        onClick = { modeTab = 1 },
                        text = { Text(stringResource(R.string.ad_gregorian)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (modeTab == 0) {
                    // BS Header: Year & Month Selectors with visible affordance and chevrons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Previous Button
                        IconButton(
                            onClick = {
                                if (selectedBsMonth > 1) {
                                    val m = selectedBsMonth - 1
                                    selectedBsMonth = m
                                    val maxDays = NepaliDateConverter.getDaysInBsMonth(selectedBsYear, m)
                                    if (selectedBsDay > maxDays) selectedBsDay = maxDays
                                    val ad = NepaliDateConverter.bsToAd(selectedBsYear, m, selectedBsDay)
                                    selectedMillis = ad.time
                                } else if (selectedBsYear > 2070) {
                                    val y = selectedBsYear - 1
                                    selectedBsYear = y
                                    selectedBsMonth = 12
                                    val maxDays = NepaliDateConverter.getDaysInBsMonth(y, 12)
                                    if (selectedBsDay > maxDays) selectedBsDay = maxDays
                                    val ad = NepaliDateConverter.bsToAd(y, 12, selectedBsDay)
                                    selectedMillis = ad.time
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
                        }

                        // Month selector dropdown
                        PickerDropdown(
                            label = stringResource(R.string.month),
                            selectedText = NepaliDateConverter.nepaliMonths.getOrElse(selectedBsMonth - 1) { "Month" },
                            items = NepaliDateConverter.nepaliMonths,
                            modifier = Modifier.weight(1.3f),
                            onItemSelected = {
                                val m = NepaliDateConverter.nepaliMonths.indexOf(it) + 1
                                selectedBsMonth = m
                                val maxDays = NepaliDateConverter.getDaysInBsMonth(selectedBsYear, m)
                                if (selectedBsDay > maxDays) selectedBsDay = maxDays
                                val ad = NepaliDateConverter.bsToAd(selectedBsYear, m, selectedBsDay)
                                selectedMillis = ad.time
                            }
                        )

                        // Year selector dropdown
                        PickerDropdown(
                            label = stringResource(R.string.year),
                            selectedText = "$selectedBsYear BS",
                            items = (2070..2090).map { "$it" },
                            modifier = Modifier.weight(1f),
                            onItemSelected = {
                                val y = it.toInt()
                                selectedBsYear = y
                                val maxDays = NepaliDateConverter.getDaysInBsMonth(y, selectedBsMonth)
                                if (selectedBsDay > maxDays) selectedBsDay = maxDays
                                val ad = NepaliDateConverter.bsToAd(y, selectedBsMonth, selectedBsDay)
                                selectedMillis = ad.time
                            }
                        )

                        // Month Next Button
                        IconButton(
                            onClick = {
                                if (selectedBsMonth < 12) {
                                    val m = selectedBsMonth + 1
                                    selectedBsMonth = m
                                    val maxDays = NepaliDateConverter.getDaysInBsMonth(selectedBsYear, m)
                                    if (selectedBsDay > maxDays) selectedBsDay = maxDays
                                    val ad = NepaliDateConverter.bsToAd(selectedBsYear, m, selectedBsDay)
                                    selectedMillis = ad.time
                                } else if (selectedBsYear < 2090) {
                                    val y = selectedBsYear + 1
                                    selectedBsYear = y
                                    selectedBsMonth = 1
                                    val maxDays = NepaliDateConverter.getDaysInBsMonth(y, 1)
                                    if (selectedBsDay > maxDays) selectedBsDay = maxDays
                                    val ad = NepaliDateConverter.bsToAd(y, 1, selectedBsDay)
                                    selectedMillis = ad.time
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full Calendar-Grid View for BS Days
                    val daysInMonth = NepaliDateConverter.getDaysInBsMonth(selectedBsYear, selectedBsMonth)
                    val adDateOfBsDay1 = NepaliDateConverter.bsToAd(selectedBsYear, selectedBsMonth, 1)
                    val bsFirstDayOfWeek = Calendar.getInstance().apply { time = adDateOfBsDay1 }.get(Calendar.DAY_OF_WEEK) - 1

                    CalendarMonthGrid(
                        firstDayOfWeek = bsFirstDayOfWeek,
                        daysInMonth = daysInMonth,
                        selectedDay = selectedBsDay,
                        onDaySelected = { day ->
                            selectedBsDay = day
                            val ad = NepaliDateConverter.bsToAd(selectedBsYear, selectedBsMonth, day)
                            selectedMillis = ad.time
                        }
                    )
                } else {
                    // AD Header: Year & Month Selectors with visible affordance and chevrons
                    val adMonths = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Previous Button
                        IconButton(
                            onClick = {
                                if (selectedAdMonth > 1) {
                                    val m = selectedAdMonth - 1
                                    selectedAdMonth = m
                                    val testCal = Calendar.getInstance().apply { set(selectedAdYear, m - 1, 1) }
                                    val maxDays = testCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                    if (selectedAdDay > maxDays) selectedAdDay = maxDays
                                    testCal.set(Calendar.DAY_OF_MONTH, selectedAdDay)
                                    selectedMillis = testCal.timeInMillis
                                } else if (selectedAdYear > 2020) {
                                    val y = selectedAdYear - 1
                                    selectedAdYear = y
                                    selectedAdMonth = 12
                                    val testCal = Calendar.getInstance().apply { set(y, 11, 1) }
                                    val maxDays = testCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                    if (selectedAdDay > maxDays) selectedAdDay = maxDays
                                    testCal.set(Calendar.DAY_OF_MONTH, selectedAdDay)
                                    selectedMillis = testCal.timeInMillis
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
                        }

                        // Month selector dropdown
                        PickerDropdown(
                            label = stringResource(R.string.month),
                            selectedText = adMonths.getOrElse(selectedAdMonth - 1) { "Month" },
                            items = adMonths,
                            modifier = Modifier.weight(1.3f),
                            onItemSelected = {
                                val m = adMonths.indexOf(it) + 1
                                selectedAdMonth = m
                                val testCal = Calendar.getInstance().apply {
                                    set(selectedAdYear, m - 1, 1)
                                }
                                val maxDays = testCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                if (selectedAdDay > maxDays) selectedAdDay = maxDays
                                testCal.set(Calendar.DAY_OF_MONTH, selectedAdDay)
                                selectedMillis = testCal.timeInMillis
                            }
                        )

                        // Year selector dropdown
                        PickerDropdown(
                            label = stringResource(R.string.year),
                            selectedText = "$selectedAdYear AD",
                            items = (2020..2035).map { "$it" },
                            modifier = Modifier.weight(1f),
                            onItemSelected = {
                                val y = it.toInt()
                                selectedAdYear = y
                                val testCal = Calendar.getInstance().apply {
                                    set(y, selectedAdMonth - 1, 1)
                                }
                                val maxDays = testCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                if (selectedAdDay > maxDays) selectedAdDay = maxDays
                                testCal.set(Calendar.DAY_OF_MONTH, selectedAdDay)
                                selectedMillis = testCal.timeInMillis
                            }
                        )

                        // Month Next Button
                        IconButton(
                            onClick = {
                                if (selectedAdMonth < 12) {
                                    val m = selectedAdMonth + 1
                                    selectedAdMonth = m
                                    val testCal = Calendar.getInstance().apply { set(selectedAdYear, m - 1, 1) }
                                    val maxDays = testCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                    if (selectedAdDay > maxDays) selectedAdDay = maxDays
                                    testCal.set(Calendar.DAY_OF_MONTH, selectedAdDay)
                                    selectedMillis = testCal.timeInMillis
                                } else if (selectedAdYear < 2035) {
                                    val y = selectedAdYear + 1
                                    selectedAdYear = y
                                    selectedAdMonth = 1
                                    val testCal = Calendar.getInstance().apply { set(y, 0, 1) }
                                    val maxDays = testCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                    if (selectedAdDay > maxDays) selectedAdDay = maxDays
                                    testCal.set(Calendar.DAY_OF_MONTH, selectedAdDay)
                                    selectedMillis = testCal.timeInMillis
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full Calendar-Grid View for AD Days
                    val firstDayCal = Calendar.getInstance().apply {
                        set(selectedAdYear, selectedAdMonth - 1, 1)
                    }
                    val adFirstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1
                    val daysInAdMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                    CalendarMonthGrid(
                        firstDayOfWeek = adFirstDayOfWeek,
                        daysInMonth = daysInAdMonth,
                        selectedDay = selectedAdDay,
                        onDaySelected = { day ->
                            selectedAdDay = day
                            val c = Calendar.getInstance().apply {
                                set(selectedAdYear, selectedAdMonth - 1, day)
                            }
                            selectedMillis = c.timeInMillis
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedMillis = System.currentTimeMillis() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.today))
                    }
                    OutlinedButton(
                        onClick = {
                            selectedMillis = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.yesterday))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bsResult = NepaliDateConverter.adToBs(selectedMillis).formatted
                    onDateSelected(selectedMillis, bsResult)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                  Text(stringResource(R.string.select))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                  Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun CalendarMonthGrid(
    firstDayOfWeek: Int, // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    daysInMonth: Int,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Week Days Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Days Grid
        val totalSlots = firstDayOfWeek + daysInMonth
        val rowCount = (totalSlots + 6) / 7

        for (row in 0 until rowCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0..6) {
                    val slotIndex = row * 7 + col
                    val dayNum = slotIndex - firstDayOfWeek + 1
                    if (dayNum in 1..daysInMonth) {
                        val isSelected = dayNum == selectedDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { onDaySelected(dayNum) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(34.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PickerDropdown(
    label: String,
    selectedText: String,
    items: List<String>,
    modifier: Modifier = Modifier,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                      contentDescription = stringResource(R.string.select_label, label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        expanded = false
                        onItemSelected(item)
                    }
                )
            }
        }
    }
}
