package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.dialogs.PdfPreviewDialog
import com.example.ui.components.DriveBackupSection
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.FinanceViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocsScreen(
    viewModel: FinanceViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currency by viewModel.currencyState.collectAsState()
    val themeMode by viewModel.themeModeState.collectAsState()
    val allowNegativeBalance by viewModel.allowNegativeBalanceState.collectAsState()
    val language by viewModel.languageState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val driveBackupState by viewModel.driveBackupState.collectAsState()

    var showJsonExportDialog by remember { mutableStateOf<String?>(null) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var jsonImportText by remember { mutableStateOf("") }

    var showCsvExportDialog by remember { mutableStateOf<String?>(null) }
    var showCsvImportDialog by remember { mutableStateOf(false) }
    var csvImportText by remember { mutableStateOf("") }
    var previewPdfFile by remember { mutableStateOf<File?>(null) }
    var showJsonTextTools by remember { mutableStateOf(false) }
    var showCsvTextTools by remember { mutableStateOf(false) }

    // Storage Access Framework: Export JSON file launcher
    val jsonExportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val json = viewModel.getExportJson()
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, context.getString(com.example.R.string.json_saved), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(com.example.R.string.file_export_failed, "JSON", e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Storage Access Framework: Import JSON file launcher
    val jsonImportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                        reader.readText()
                    } ?: ""
                    if (content.isNotBlank()) {
                        viewModel.importJson(content) { result ->
                            result.fold(
                                onSuccess = { count -> Toast.makeText(context, context.getString(com.example.R.string.restore_records, count), Toast.LENGTH_SHORT).show() },
                                onFailure = { error -> Toast.makeText(context, context.getString(com.example.R.string.restore_failed, error.message ?: ""), Toast.LENGTH_LONG).show() }
                            )
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(com.example.R.string.file_read_failed, "JSON", e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Storage Access Framework: Export CSV file launcher
    val csvExportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val csv = viewModel.getExportCsv()
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, context.getString(com.example.R.string.csv_exported), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(com.example.R.string.file_export_failed, "CSV", e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Storage Access Framework: Import CSV file launcher
    val csvImportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                        reader.readText()
                    } ?: ""
                    if (content.isNotBlank()) {
                        viewModel.importCsv(content)
                        Toast.makeText(context, context.getString(com.example.R.string.csv_imported), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(com.example.R.string.file_read_failed, "CSV", e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(com.example.R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(com.example.R.string.settings_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(com.example.R.string.private_backups),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(com.example.R.string.private_backups_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            DriveBackupSection(
                state = driveBackupState,
                onConnect = viewModel::connectDriveAccount,
                onDisconnect = {
                    viewModel.disconnectDriveAccount()
                    GoogleSignIn.getClient(
                        context,
                        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    ).signOut()
                },
                onAutomaticChanged = viewModel::setAutomaticDriveBackup,
                onBackupNow = viewModel::backupToDrive,
                onRestore = viewModel::restoreFromDrive
            )
        }

        // CARD 1: Reports & Data Backup (Consolidated with internal dividers)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // SECTION: PDF Statement Generator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(com.example.R.string.export_statement_pdf),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(com.example.R.string.pdf_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.generatePdfStatement { pdfFile ->
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    val fileName = "Statement_$dateStr.pdf"
                                    var saved = false
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            val values = ContentValues().apply {
                                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                            }
                                            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                                            if (uri != null) {
                                                context.contentResolver.openOutputStream(uri)?.use { out ->
                                                    pdfFile.inputStream().use { input -> input.copyTo(out) }
                                                }
                                                saved = true
                                            }
                                        } else {
                                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                            downloadsDir.mkdirs()
                                            val destFile = File(downloadsDir, fileName)
                                            pdfFile.copyTo(destFile, overwrite = true)
                                            saved = true
                                        }
                                    } catch (_: Exception) {
                                        saved = false
                                    }
                                    if (saved) {
                                        Toast.makeText(context, context.getString(com.example.R.string.saved_to_downloads, fileName), Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, context.getString(com.example.R.string.pdf_save_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(com.example.R.string.save_pdf), fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.generatePdfStatement { pdfFile ->
                                    previewPdfFile = pdfFile
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                            border = BorderStroke(1.5.dp, EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(com.example.R.string.preview_pdf), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // SECTION: JSON Full Backup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataObject,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(com.example.R.string.full_database_backup),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(com.example.R.string.database_backup_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Equal-width paired pill buttons for JSON file operations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                jsonExportFileLauncher.launch("finance_backup_${System.currentTimeMillis()}.json")
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(com.example.R.string.save_file), fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                jsonImportFileLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                            border = BorderStroke(1.5.dp, EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(com.example.R.string.open_file), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Collapsible secondary text actions with centered alignment under parent buttons
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showJsonTextTools = !showJsonTextTools }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showJsonTextTools) stringResource(com.example.R.string.hide_clipboard_tools) else stringResource(com.example.R.string.clipboard_text_tools),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (showJsonTextTools) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = showJsonTextTools) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val json = viewModel.getExportJson()
                                            showJsonExportDialog = json
                                        }
                                    },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(com.example.R.string.view_text), fontSize = 12.sp)
                                }
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                TextButton(
                                    onClick = { showJsonImportDialog = true },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(com.example.R.string.paste_text), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // SECTION: CSV Transactions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFD1FAE5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(com.example.R.string.transactions_csv),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(com.example.R.string.csv_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Equal-width paired pill buttons for CSV file operations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                csvExportFileLauncher.launch("transactions_${System.currentTimeMillis()}.csv")
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(com.example.R.string.export_file), fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                csvImportFileLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*"))
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                            border = BorderStroke(1.5.dp, EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(com.example.R.string.import_file), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Collapsible secondary text actions with centered alignment under parent buttons
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCsvTextTools = !showCsvTextTools }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showCsvTextTools) stringResource(com.example.R.string.hide_clipboard_tools) else stringResource(com.example.R.string.clipboard_csv_tools),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (showCsvTextTools) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = showCsvTextTools) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val csv = viewModel.getExportCsv()
                                            showCsvExportDialog = csv
                                        }
                                    },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(com.example.R.string.view_csv), fontSize = 12.sp)
                                }
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                TextButton(
                                    onClick = { showCsvImportDialog = true },
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(com.example.R.string.paste_csv), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD 2: App Preferences
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(com.example.R.string.app_settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Currency Switcher
                    Column {
                        Text(stringResource(com.example.R.string.default_currency), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currency == "NPR",
                                onClick = { viewModel.setCurrency("NPR") },
                                label = { Text(stringResource(com.example.R.string.npr_currency)) }
                            )
                            FilterChip(
                                selected = currency == "USD",
                                onClick = { viewModel.setCurrency("USD") },
                                label = { Text(stringResource(com.example.R.string.usd_currency)) }
                            )
                        }
                    }

                    // Language Selector
                    Column {
                        Text(stringResource(com.example.R.string.language), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = language == "en",
                                onClick = {
                                    viewModel.setLanguage("en")
                                    (context as? android.app.Activity)?.recreate()
                                },
                                label = { Text(stringResource(com.example.R.string.english)) }
                            )
                            FilterChip(
                                selected = language == "ne",
                                onClick = {
                                    viewModel.setLanguage("ne")
                                    (context as? android.app.Activity)?.recreate()
                                },
                                label = { Text(stringResource(com.example.R.string.nepali)) }
                            )
                        }
                    }

                    // Theme Selector
                    Column {
                        Text(stringResource(com.example.R.string.theme_mode), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = themeMode == "SYSTEM",
                                onClick = { viewModel.setThemeMode("SYSTEM") },
                                label = { Text(stringResource(com.example.R.string.system)) }
                            )
                            FilterChip(
                                selected = themeMode == "LIGHT",
                                onClick = { viewModel.setThemeMode("LIGHT") },
                                label = { Text(stringResource(com.example.R.string.light)) }
                            )
                            FilterChip(
                                selected = themeMode == "DARK",
                                onClick = { viewModel.setThemeMode("DARK") },
                                label = { Text(stringResource(com.example.R.string.dark)) }
                            )
                        }
                    }

                    HorizontalDivider()

                    // Toggle Row with Description Text:
                    // Align toggle control vertically centered to the title line only (first line),
                    // not the full paragraph block, so multi-line descriptions don't visually misalign the control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.height(32.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(com.example.R.string.allow_negative_balances),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(com.example.R.string.negative_balance_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = allowNegativeBalance,
                            onCheckedChange = { viewModel.setAllowNegativeBalance(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            ),
                            modifier = Modifier.padding(top = 0.dp)
                        )
                    }
                }
            }
        }

        // CARD 3: Account Profile & Sign Out
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(com.example.R.string.account_profile),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = authState.currentUser?.name ?: "Offline User",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = authState.currentUser?.username ?: "demo_user",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(com.example.R.string.log_out), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // CARD 4: About Sfinance & Developer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(com.example.R.string.about_sfinance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(com.example.R.string.developer_name),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(com.example.R.string.about_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(com.example.R.string.version),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // JSON Export Dialog
    showJsonExportDialog?.let { json ->
        AlertDialog(
            onDismissRequest = { showJsonExportDialog = null },
            title = { Text(stringResource(com.example.R.string.exported_json)) },
            text = {
                Column {
                    Text(stringResource(com.example.R.string.copy_share_json), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = json.take(1000) + if (json.length > 1000) "\n... (${json.length} characters)" else "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(com.example.R.string.json_clipboard_label), json))
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share JSON Backup"))
                        showJsonExportDialog = null
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(stringResource(com.example.R.string.copy_share))
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonExportDialog = null }) {
                    Text(stringResource(com.example.R.string.close))
                }
            }
        )
    }

    // JSON Import Dialog
    if (showJsonImportDialog) {
        AlertDialog(
            onDismissRequest = { showJsonImportDialog = false },
            title = { Text(stringResource(com.example.R.string.import_json_backup)) },
            text = {
                Column {
                    Text(stringResource(com.example.R.string.paste_json), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonImportText,
                        onValueChange = { jsonImportText = it },
                        modifier = Modifier.height(200.dp),
                        placeholder = { Text(stringResource(com.example.R.string.json_placeholder)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonImportText.isNotBlank()) {
                            viewModel.importJson(jsonImportText) { result ->
                                result.fold(
                                    onSuccess = { count -> Toast.makeText(context, "Restored $count records for this user", Toast.LENGTH_SHORT).show() },
                                    onFailure = { error -> Toast.makeText(context, "Restore failed: ${error.message}", Toast.LENGTH_LONG).show() }
                                )
                            }
                            showJsonImportDialog = false
                            jsonImportText = ""
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(stringResource(com.example.R.string.import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonImportDialog = false }) {
                    Text(stringResource(com.example.R.string.cancel))
                }
            }
        )
    }

    // CSV Export Dialog
    showCsvExportDialog?.let { csv ->
        AlertDialog(
            onDismissRequest = { showCsvExportDialog = null },
            title = { Text(stringResource(com.example.R.string.exported_csv)) },
            text = {
                Column {
                    Text(stringResource(com.example.R.string.copy_share_csv), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = csv.take(1000) + if (csv.length > 1000) "\n... (${csv.length} characters)" else "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(com.example.R.string.csv_clipboard_label), csv))
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Transactions CSV"))
                        showCsvExportDialog = null
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(stringResource(com.example.R.string.copy_share))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvExportDialog = null }) {
                    Text(stringResource(com.example.R.string.close))
                }
            }
        )
    }

    // CSV Import Dialog
    if (showCsvImportDialog) {
        AlertDialog(
            onDismissRequest = { showCsvImportDialog = false },
            title = { Text(stringResource(com.example.R.string.import_csv_backup)) },
            text = {
                Column {
                    Text(stringResource(com.example.R.string.paste_csv_content), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = csvImportText,
                        onValueChange = { csvImportText = it },
                        modifier = Modifier.height(200.dp),
                        placeholder = { Text(stringResource(com.example.R.string.csv_placeholder)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (csvImportText.isNotBlank()) {
                            viewModel.importCsv(csvImportText)
                            showCsvImportDialog = false
                            csvImportText = ""
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(stringResource(com.example.R.string.import_csv))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvImportDialog = false }) {
                    Text(stringResource(com.example.R.string.cancel))
                }
            }
        )
    }

    // PDF Preview Modal
    previewPdfFile?.let { file ->
        PdfPreviewDialog(
            pdfFile = file,
            onDismiss = { previewPdfFile = null },
            onShare = {
                try {
                    val uri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Statement PDF"))
                } catch (_: Exception) {}
            }
        )
    }
}
